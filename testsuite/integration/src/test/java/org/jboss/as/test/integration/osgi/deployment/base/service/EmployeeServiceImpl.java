package org.jboss.as.test.integration.osgi.deployment.base.service;

import org.jboss.as.test.integration.osgi.deployment.base.model.Employee;

/**
 * Created by satish on 13/10/16.
 */
public class EmployeeServiceImpl implements EmployeeService {
    @Override
    public Employee getEmployee() {

        Employee employee = new Employee();
        employee.setId(1);
        employee.setFirstName("Satish");
        employee.setLastName("Bhor");
        employee.setAddress("Pune, India");
        return employee;
    }
}
