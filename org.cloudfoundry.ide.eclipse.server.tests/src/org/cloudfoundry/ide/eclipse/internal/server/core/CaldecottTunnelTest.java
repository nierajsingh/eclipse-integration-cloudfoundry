/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.core;

import org.cloudfoundry.caldecott.client.TunnelHelper;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture;
import org.cloudfoundry.ide.eclipse.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

public class CaldecottTunnelTest extends AbstractCloudFoundryServicesTest {
	public static final String MYSQL_SERVICE_NAME = "mysqlCaldecottTestService";

	public static final String MONGODB_SERVICE_NAME = "mongodbCaldecottTestService";

	public static final String POSTGRESQL_SERVICE_NAME = "postgresqlCaldecottTestService";

	public static final String LOCAL_HOST = "127.0.0.1";

	@Override
	protected Harness createHarness() {
		return CloudFoundryTestFixture.current().harness();
	}

	public void testCreateMysqlTunnel() throws Exception {
		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);
		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MYSQL_SERVICE_NAME);

		String expectedURL = "jdbc:mysql://" + LOCAL_HOST + ":" + descriptor.tunnelPort() + "/"
				+ descriptor.getDatabaseName();
		assertEquals(expectedURL, descriptor.getURL());

		stopTunnel(MYSQL_SERVICE_NAME);
		assertNoTunnel(MYSQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);
	}

	public void testCreateMongodbTunnel() throws Exception {
		CloudService service = getMongodbService();
		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MONGODB_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MONGODB_SERVICE_NAME);

		stopTunnel(MONGODB_SERVICE_NAME);
		assertNoTunnel(MONGODB_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MONGODB_SERVICE_NAME);
	}

	public void testCreatePostgresqlTunnel() throws Exception {
		CloudService service = getPostgresqlService();
		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(POSTGRESQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(POSTGRESQL_SERVICE_NAME);

		String expectedURL = "jdbc:postgresql://" + LOCAL_HOST + ":" + descriptor.tunnelPort() + "/"
				+ descriptor.getDatabaseName();
		assertEquals(expectedURL, descriptor.getURL());
		stopTunnel(POSTGRESQL_SERVICE_NAME);
		assertNoTunnel(POSTGRESQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(POSTGRESQL_SERVICE_NAME);
	}

	public void testCaldecottTunnelCloseOnServiceDeletion() throws Exception {
		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);
		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MYSQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);
		assertNoTunnel(MYSQL_SERVICE_NAME);
	}

	/*
	 * Test that when a non caldecott app has a service unbound, and that
	 * service also has a Caldecott tunnel, the tunnel does not get closed.
	 */
	public void testNonCaldecottServiceUnbinding_STS_2767() throws Exception {
		CloudApplication nonCaldecottApp = createAndAssertTestApp();
		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);

		assertStopApplication(nonCaldecottApp);

		bindServiceToApp(nonCaldecottApp, service);

		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MYSQL_SERVICE_NAME);

		assertStartApplication(nonCaldecottApp);
		assertServiceBound(service.getName(), nonCaldecottApp);

		assertStopApplication(nonCaldecottApp);
		unbindServiceToApp(nonCaldecottApp, service);
		assertServiceNotBound(service.getName(), nonCaldecottApp);

		assertTunnel(MYSQL_SERVICE_NAME);

		stopTunnel(MYSQL_SERVICE_NAME);
		assertNoTunnel(MYSQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);

		assertRemoveApplication(nonCaldecottApp);

	}

	public void testTunnelCloseOnCaldecottStop() throws Exception {

		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);

		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MYSQL_SERVICE_NAME);

		CloudApplication caldecottApp = getCaldecottApplication();
		assertNotNull(caldecottApp);

		assertStopApplication(caldecottApp);

		assertNoTunnel(MYSQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);
	}

	public void testTunnelCloseOnCaldecottDeletion() throws Exception {

		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);

		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MYSQL_SERVICE_NAME);

		CloudApplication caldecottApp = getCaldecottApplication();
		assertNotNull(caldecottApp);

		assertRemoveApplication(caldecottApp);

		assertNoTunnel(MYSQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);
	}

	public void testTunnelCloseOnCaldecottServiceUnbinding() throws Exception {
		CloudService service = getMysqlService();
		assertServiceExists(MYSQL_SERVICE_NAME);

		CaldecottTunnelDescriptor descriptor = createCaldecottTunnel(MYSQL_SERVICE_NAME);
		assertNotNull(descriptor);
		assertTunnel(MYSQL_SERVICE_NAME);

		CloudApplication caldecottApp = getCaldecottApplication();
		assertNotNull(caldecottApp);

		unbindServiceToApp(caldecottApp, service);
		assertServiceNotBound(service.getName(), caldecottApp);

		assertNoTunnel(MYSQL_SERVICE_NAME);

		deleteService(service);
		assertServiceNotExist(MYSQL_SERVICE_NAME);
	}

	/*
	 * 
	 * HELPERS
	 */

	protected CloudApplication getCaldecottApplication() throws CoreException {
		return getUpdatedApplication(TunnelHelper.getTunnelAppName());
	}

	protected void stopTunnel(String serviceName) throws CoreException {
		CaldecottTunnelHandler handler = new CaldecottTunnelHandler(cloudServer);
		handler.stopAndDeleteCaldecottTunnel(serviceName, new NullProgressMonitor());

	}

	protected void assertNoTunnel(String serviceName) throws Exception {
		CaldecottTunnelHandler handler = new CaldecottTunnelHandler(cloudServer);
		assertFalse(handler.hasCaldecottTunnel(serviceName));
	}

	protected void assertTunnel(String serviceName) throws Exception {
		CaldecottTunnelHandler handler = new CaldecottTunnelHandler(cloudServer);
		assertTrue(handler.hasCaldecottTunnel(serviceName));
	}

	protected CloudService getMysqlService() throws Exception {
		CloudService service = getCloudService(MYSQL_SERVICE_NAME);
		if (service == null) {
			service = createCloudService(MYSQL_SERVICE_NAME, "mysql");
		}
		return service;
	}

	protected CloudService getMongodbService() throws Exception {
		CloudService service = getCloudService(MONGODB_SERVICE_NAME);
		if (service == null) {
			service = createCloudService(MONGODB_SERVICE_NAME, "mongodb");
		}
		return service;
	}

	protected CloudService getPostgresqlService() throws Exception {
		CloudService service = getCloudService(POSTGRESQL_SERVICE_NAME);
		if (service == null) {
			service = createCloudService(POSTGRESQL_SERVICE_NAME, "postgresql");
		}
		return service;
	}

	protected CaldecottTunnelDescriptor createCaldecottTunnel(String serviceName) throws CoreException {
		CaldecottTunnelHandler handler = new CaldecottTunnelHandler(cloudServer);
		return handler.startCaldecottTunnel(serviceName, new NullProgressMonitor());
	}

}