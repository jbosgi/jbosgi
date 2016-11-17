package org.jboss.as.test.integration.osgi.deployment.dependent.model;

import org.jboss.as.test.integration.osgi.deployment.base.model.Employee;

import java.util.List;

/**
 * Created by satish on 13/10/16.
 */
public class Organization {
    private int id;
    private String name;
    private List<Employee> employees;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }
}
