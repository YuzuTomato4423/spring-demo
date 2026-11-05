package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import com.example.demo.model.Employee;
import com.example.demo.model.Department;

public interface EmployeeRepository {
	void save(Employee employee);

    Employee insert(String name, Department department);
    boolean replace(Employee employee);
    boolean deleteById(int id);
	
	Optional<Employee> findById(int id);
	
	List<Employee> findAll();
}
