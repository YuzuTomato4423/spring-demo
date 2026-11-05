package com.example.demo;

import com.example.demo.model.Department;
import com.example.demo.model.Employee;

public class EmployeeTest {

    public static void main(String[] args) {

        Department department =
                new Department(1, "開発部");

        Employee employee =
                new Employee(100, "田中", department);

        EmployeeFormatter formatter =
                new SimpleEmployeeFormatter();

        String result = formatter.format(employee);

        System.out.println(result);
    }
}