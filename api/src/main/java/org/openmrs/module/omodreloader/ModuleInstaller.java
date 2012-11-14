/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.omodreloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.ModuleFileParser;
import org.openmrs.module.ModuleUtil;
import org.openmrs.module.web.WebModuleUtil;
import org.openmrs.module.web.controller.ModuleListController;
import org.openmrs.util.OpenmrsConstants;

/**
 * Installs or upgrades modules. The code is copied from {@link ModuleListController}.
 */
public class ModuleInstaller {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	public void install(InputStream inputStream, String filename, String username, String password) {
		log.info("Installing module " + filename);
		
		FileInputStream fileInputStream = null;
		try {
			Context.openSession();
			
			if (username != null) {
				Context.authenticate(username, password);
			}
			
			Context.addProxyPrivilege(OpenmrsConstants.PRIV_MANAGE_GLOBAL_PROPERTIES);
			Context.addProxyPrivilege(OpenmrsConstants.PRIV_MANAGE_SCHEDULER);
			
			
			Module tmpModule = new ModuleFileParser(inputStream).parse();
			Module existingModule = ModuleFactory.getModuleById(tmpModule.getModuleId());
			List<Module> dependentModulesStopped = null;
			
			if (existingModule != null) {
				log.info("Stopping existing module " + existingModule.getModuleId());
				dependentModulesStopped = ModuleFactory.stopModule(existingModule, false, true);
				
				for (Module depMod : dependentModulesStopped) {
					log.info("Stopping dependent module " + depMod.getModuleId());
					WebModuleUtil.stopModule(depMod, ServletContextHolder.servletContext);
				}
				
				WebModuleUtil.stopModule(existingModule, ServletContextHolder.servletContext);
				ModuleFactory.unloadModule(existingModule);
			}
			fileInputStream = new FileInputStream(tmpModule.getFile());
			File moduleFile = ModuleUtil.insertModuleFile(fileInputStream, filename);
			Module module = ModuleFactory.loadModule(moduleFile);
			
			if (module != null) {
				ModuleFactory.startModule(module);
				boolean contextRefreshNeeded = false;
				
				log.info("Starting module " + module.getModuleId());
				boolean moduleNeedsContextRefresh = WebModuleUtil.startModule(module, ServletContextHolder.servletContext, true);
				if (moduleNeedsContextRefresh) {
					contextRefreshNeeded = true;
				}
				
				if (module.isStarted()) {
					if (existingModule != null && dependentModulesStopped != null) {
						for (Module depMod : sortStartupOrder(dependentModulesStopped)) {
							log.info("Starting dependent module " + depMod.getModuleId());
							ModuleFactory.startModule(depMod);
							
							moduleNeedsContextRefresh = WebModuleUtil.startModule(depMod, ServletContextHolder.servletContext, true);
							if (moduleNeedsContextRefresh) {
								contextRefreshNeeded = true;
							}
						}
					}
				}
				
				//We deferred refreshing context so that it happened only once for all modules.
				if (contextRefreshNeeded) {
					try {
						WebModuleUtil.refreshWAC(ServletContextHolder.servletContext);
					} catch (NoSuchMethodError e) {
						//The method signature has changed since 1.8
						Method method = WebModuleUtil.class.getMethod("refreshWAC", ServletContext.class, boolean.class, Module.class);
						method.invoke(WebModuleUtil.class, ServletContextHolder.servletContext, true, null);
					}
				}
				
				//We need to load servlets and filters, because we delayed refreshing context.
				WebModuleUtil.loadServlets(module, ServletContextHolder.servletContext);
				WebModuleUtil.loadFilters(module, ServletContextHolder.servletContext);
				
				for (Module depMod : dependentModulesStopped) {
					WebModuleUtil.loadServlets(depMod, ServletContextHolder.servletContext);
					WebModuleUtil.loadFilters(depMod, ServletContextHolder.servletContext);
				}
			}
			
			inputStream.close();
			fileInputStream.close();
			
			log.info("Finished installing module " + filename);
		}
		catch (Exception e) {
			log.error("Failed to install module " + filename, e);
		}
		finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(fileInputStream);
			
			Context.removeProxyPrivilege(OpenmrsConstants.PRIV_MANAGE_GLOBAL_PROPERTIES);
			Context.removeProxyPrivilege(OpenmrsConstants.PRIV_MANAGE_SCHEDULER);
			Context.clearSession();
			Context.closeSession();
		}
	}
	
	/**
	 * @param modulesToStart
	 * @return a new list, with the same elements as modulesToStart, sorted so that no module is
	 *         before a module it depends on
	 * @should sort modules correctly
	 */
	protected List<Module> sortStartupOrder(List<Module> modulesToStart) {
		// can't use Collections.sort--we need a slower algorithm that guarantees to compare every pair of elements
		List<Module> candidates = new LinkedList<Module>(modulesToStart);
		List<Module> ret = new ArrayList<Module>();
		while (candidates.size() > 0) {
			Module mod = removeModuleWithNoDependencies(candidates);
			if (mod == null) {
				log.warn("Unable to determine suitable startup order for " + modulesToStart);
				return modulesToStart;
			}
			ret.add(mod);
		}
		return ret;
	}
	
	/**
	 * Looks for a module in the list that doesn't depend on any other modules in the list. If any
	 * is found, that module is removed from the list and returned.
	 * 
	 * @param candidates
	 * @return
	 */
	protected Module removeModuleWithNoDependencies(List<Module> candidates) {
		for (Iterator<Module> i = candidates.iterator(); i.hasNext();) {
			Module candidate = i.next();
			boolean suitable = true;
			for (Module other : candidates) {
				if (candidate.getRequiredModules().contains(other.getPackageName())) {
					suitable = false;
					break;
				}
			}
			if (suitable) {
				i.remove();
				return candidate;
			}
		}
		return null;
	}
}
