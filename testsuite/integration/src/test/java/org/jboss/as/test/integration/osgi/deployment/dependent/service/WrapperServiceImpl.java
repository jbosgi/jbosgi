package org.jboss.as.test.integration.osgi.deployment.dependent.service;

import org.jboss.as.test.integration.osgi.deployment.base.model.Employee;
import org.jboss.as.test.integration.osgi.deployment.base.service.EmployeeService;
import org.jboss.as.test.integration.osgi.deployment.dependent.model.Organization;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by satish on 13/10/16.
 */
public class WrapperServiceImpl implements WrapperService {

    private BundleContext bundleContext;

    public WrapperServiceImpl(BundleContext bundleContext){
        this.bundleContext = bundleContext;
    }


    public Organization getOrganizationEmployee() {

       ServiceReference serviceReference = bundleContext.getServiceReference(EmployeeService.class);
       EmployeeService employeeService = (EmployeeService) bundleContext.getService(serviceReference);

       Employee employee = employeeService.getEmployee();
       Organization organization = new Organization();
       organization.setId(1);
       organization.setName("Test");
       organization.setEmployees(Arrays.asList(employee));

        return organization;
    }
}
