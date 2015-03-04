/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.cloudfoundry.ide.eclipse.server.core.internal.jrebel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.CloudServerListener;
import org.cloudfoundry.ide.eclipse.server.core.internal.ServerEventHandler;
import org.cloudfoundry.ide.eclipse.server.core.internal.application.ModuleChangeEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.AppUrlChangeEvent;
import org.cloudfoundry.ide.eclipse.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;

public class CloudRebelAppHandler implements CloudServerListener {

	private static CloudRebelAppHandler handler;

	private CloudRebelAppHandler() {
		ServerEventHandler.getDefault().addServerListener(this);
	}

	public static void init() {
		if (handler == null) {
			handler = new CloudRebelAppHandler();
		}
	}

	protected IProject getRebelProject(IModule module) {
		if (module == null) {
			return null;
		}
		IProject project = module.getProject();
		try {
			if (project != null && project.isAccessible()
					&& project.hasNature("org.zeroturnaround.eclipse.remoting.remotingNature") //$NON-NLS-1$
					&& project.hasNature("org.zeroturnaround.eclipse.jrebelNature")) { //$NON-NLS-1$
				return project;
			}
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}

		return null;
	}

	@Override
	public void serverChanged(CloudServerEvent event) {

		if (event instanceof ModuleChangeEvent
				&& (event.getType() == CloudServerEvent.EVENT_APP_DEPLOYMENT_CHANGED || event.getType() == CloudServerEvent.EVENT_APP_URL_CHANGED)) {

			IModule module = ((ModuleChangeEvent) event).getModule();

			IProject project = getRebelProject(module);

			if (project != null) {
				handleRebelProject((ModuleChangeEvent) event, project);
			}
		}
	}

	protected void handleRebelProject(ModuleChangeEvent event, IProject project) {
		CloudFoundryServer server = event.getServer();

		try {
			Class<?> providerClass = Class.forName("org.zeroturnaround.eclipse.jrebel.remoting.RebelRemotingProvider"); //$NON-NLS-1$
			if (providerClass != null) {

				Method getRemotingProject = providerClass.getMethod("getRemotingProject", IProject.class); //$NON-NLS-1$

				if (getRemotingProject != null) {

					getRemotingProject.setAccessible(true);

					// static method
					Object remoteProjectObj = getRemotingProject.invoke(null, project);
					if (remoteProjectObj != null
							&& remoteProjectObj.getClass().getName()
									.equals("org.zeroturnaround.eclipse.jrebel.remoting.RemotingProject")) { //$NON-NLS-1$

						URL[] existingRebelUrls = null;
						Method getUrls = remoteProjectObj.getClass().getMethod("getRemoteUrls"); //$NON-NLS-1$
						if (getUrls != null) {
							getUrls.setAccessible(true);
							Object urlList = getUrls.invoke(remoteProjectObj);
							if (urlList instanceof URL[]) {
								existingRebelUrls = (URL[]) urlList;
							}
						}

						List<String> appUrls = null;
						List<String> oldAppUrls = null;
						if (event instanceof AppUrlChangeEvent) {
							AppUrlChangeEvent appUrlEvent = (AppUrlChangeEvent) event;
							appUrls = appUrlEvent.getChangedUrls();
							oldAppUrls = appUrlEvent.getOldUrls();
						}
						else {
							CloudFoundryApplicationModule appModule = server.getExistingCloudModule(event.getModule());
							if (appModule != null && appModule.getDeploymentInfo() != null) {
								appUrls = appModule.getDeploymentInfo().getUris();
							}
						}
						if (existingRebelUrls == null) {
							existingRebelUrls = new URL[0];
						}

						List<URL> updatedRebelUrls = new ArrayList<URL>();

						// Remove old app URLs
						for (URL rebelUrl : existingRebelUrls) {
							String authority = rebelUrl.getAuthority();
							if (oldAppUrls == null || !oldAppUrls.contains(authority)) {
								updatedRebelUrls.add(rebelUrl);
							}
						}

						// Add new app URLs
						if (appUrls != null) {
							for (String appUrl : appUrls) {
								if (!appUrl.startsWith("http://") || !appUrl.startsWith("https://")) { //$NON-NLS-1$ //$NON-NLS-2$
									appUrl = "http://" + appUrl; //$NON-NLS-1$
								}
								try {
									URL appURL = new URL(appUrl);
									if (!updatedRebelUrls.contains(appURL)) {
										updatedRebelUrls.add(appURL);
									}
								}
								catch (MalformedURLException e) {
									CloudFoundryPlugin.logError(e);
								}
							}
						}

						Method setUrls = remoteProjectObj.getClass().getDeclaredMethod("setRemoteUrls", URL[].class); //$NON-NLS-1$

						if (setUrls != null) {
							setUrls.setAccessible(true);
							setUrls.invoke(remoteProjectObj, new Object[] { updatedRebelUrls.toArray(new URL[0]) });
						}
					}
				}
			}
		}
		catch (ClassNotFoundException e) {
			// JRebel may not be available. Ignore
		}
		// Log all other errors
		catch (SecurityException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (NoSuchMethodException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (IllegalAccessException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (InvocationTargetException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (IllegalArgumentException e) {
			CloudFoundryPlugin.logError(e);
		}
	}
}
