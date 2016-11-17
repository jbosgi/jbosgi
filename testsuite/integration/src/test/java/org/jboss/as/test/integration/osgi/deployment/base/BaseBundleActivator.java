package org.jboss.as.test.integration.osgi.deployment.base;

import org.jboss.as.test.integration.osgi.deployment.base.service.EmployeeService;
import org.jboss.as.test.integration.osgi.deployment.base.service.EmployeeServiceImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Created by satish on 13/10/16.
 */
public class BaseBundleActivator implements BundleActivator {

    @Override
    public void start(BundleContext bundleContext) throws Exception {

        emmployeeService = bundleContext.registerService(EmployeeService.class.getName(), new EmployeeServiceImpl(), null);

    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

        emmployeeService.unregister();

    }

    private ServiceRegistration emmployeeService;
}
