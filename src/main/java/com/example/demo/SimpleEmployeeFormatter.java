package com.example.demo;

import com.example.demo.model.Employee;

public class SimpleEmployeeFormatter implements EmployeeFormatter {
	@Override
	public String format(Employee employee) {
		return employee.getId()
			+ ":"
			+ employee.getName()
			+ "/"
			+employee.getDepartment().getName();
	}
}
