package org.jboss.as.test.integration.osgi.deployment.dependent;

import org.jboss.as.test.integration.osgi.deployment.base.service.EmployeeService;
import org.jboss.as.test.integration.osgi.deployment.base.service.EmployeeServiceImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Created by satish on 13/10/16.
 */
public class DependentBundleActivator implements BundleActivator {

    public void start(BundleContext bundleContext) throws Exception {
        wrapperService = bundleContext.registerService(EmployeeService.class.getName(), new EmployeeServiceImpl(), null);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        wrapperService.unregister();
    }

    private ServiceRegistration wrapperService;
}
