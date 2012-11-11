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

import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileSystemException;

/**
 * Installs or upgrades modules.
 */
public class ModuleFileListener implements FileListener {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * @see org.apache.commons.vfs2.FileListener#fileChanged(org.apache.commons.vfs2.FileChangeEvent)
	 */
	@Override
	public void fileChanged(FileChangeEvent event) throws Exception {
		installModule(event);
	}
	
	/**
	 * @see org.apache.commons.vfs2.FileListener#fileCreated(org.apache.commons.vfs2.FileChangeEvent)
	 */
	@Override
	public void fileCreated(FileChangeEvent event) throws Exception {
		installModule(event);
	}
	
	private void installModule(FileChangeEvent event) throws FileSystemException {
		if ("omod".equals(event.getFile().getName().getExtension())) {
			InputStream inputStream = event.getFile().getContent().getInputStream();
			String filename = event.getFile().getName().getBaseName();
			new ModuleInstaller().install(inputStream, filename);
		}
	}
	
	/**
	 * @see org.apache.commons.vfs2.FileListener#fileDeleted(org.apache.commons.vfs2.FileChangeEvent)
	 */
	@Override
	public void fileDeleted(FileChangeEvent event) throws Exception {
	}
	
}
