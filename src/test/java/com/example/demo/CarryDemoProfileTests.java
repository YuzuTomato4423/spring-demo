package com.example.demo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.service.EmployeeService;

@SpringBootTest
@ActiveProfiles("carry-demo")
class CarryDemoProfileTests {
    @Autowired EmployeeService service;
    @Autowired CarryDemoRunner runner;

    @Test
    void demoRunsWithSixEmployeesAndSharedSingleton() {
        assertNotNull(runner);
        assertEquals(6, service.findAll().size());
        assertEquals("山田", service.findById(6).getName());
        assertEquals(4, service.findByDepartmentId(1).size());
        assertEquals(2, service.findByDepartmentId(2).size());
        assertEquals(1, service.getCounter());
    }
}
