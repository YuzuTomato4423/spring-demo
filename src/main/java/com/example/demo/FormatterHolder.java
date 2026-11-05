package com.example.demo;

public class FormatterHolder {

    private final EmployeeFormatter employeeFormatter;

    public FormatterHolder(EmployeeFormatter employeeFormatter) {
        this.employeeFormatter = employeeFormatter;
    }

    public EmployeeFormatter getEmployeeFormatter() {
        return employeeFormatter;
    }
}