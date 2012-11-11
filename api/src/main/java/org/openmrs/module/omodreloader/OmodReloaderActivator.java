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


import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.openmrs.module.Activator;

/**
 * This class contains the logic that is run every time this module is either started or stopped.
 */
public class OmodReloaderActivator implements Activator {
	
	protected Log log = LogFactory.getLog(getClass());
	
	private static volatile DefaultFileMonitor fileMonitor;
		
	/**
	 * @see Activator#startup()
	 */
	public void startup() {
		log.info("Starting Omod Reloader Module");
		
		try {
	        FileSystemManager fsManager = VFS.getManager();
	        
	        String pathsToScan = System.getProperty("omodreloader.paths");
	        if (StringUtils.isBlank(pathsToScan)) {
	        	return;
	        }
	        
	        String[] paths = pathsToScan.split(";");
	        
	        fileMonitor = new DefaultFileMonitor(new ModuleFileListener());
	        fileMonitor.setRecursive(false);
	        for (String path : paths) {
	            FileObject file = fsManager.resolveFile(path);
	            fileMonitor.addFile(file);
            }
	        fileMonitor.start();
        }
        catch (FileSystemException e) {
	        throw new RuntimeException(e);
        }
	}
	
	/**
	 * @see Activator#shutdown()
	 */
	public void shutdown() {
		log.info("Shutting down Omod Reloader Module");
		fileMonitor.stop();
		fileMonitor = null;
	}
	
		
}
