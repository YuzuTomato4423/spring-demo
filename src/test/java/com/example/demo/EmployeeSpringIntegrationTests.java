package com.example.demo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.controller.EmployeeController;
import com.example.demo.controller.Week4Day2EmployeeController;
import com.example.demo.dto.EmployeeRequest;
import com.example.demo.exception.EmployeeNotFoundException;
import com.example.demo.mapper.EmployeeMapper;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.service.EmployeeService;

@SpringBootTest
class EmployeeSpringIntegrationTests {
    @Autowired ApplicationContext context;
    @Autowired EmployeeController controller;
    @Autowired EmployeeService service;
    @Autowired EmployeeRepository repository;
    @Autowired EmployeeMapper mapper;

    @Test
    void uniqueBeansAreTheActualInjectedDependencies() {
        for (Class<?> type : new Class<?>[] { EmployeeController.class, EmployeeService.class,
                EmployeeRepository.class, EmployeeMapper.class }) {
            assertEquals(1, context.getBeansOfType(type).size());
        }
        assertSame(service, ReflectionTestUtils.getField(controller, "service"));
        // SPR-W8-D1-01: 公開DTOを作るHTTPコントローラでMapperの配線を確認する。
        var responseController = context.getBean(Week4Day2EmployeeController.class);
        assertSame(mapper, ReflectionTestUtils.getField(responseController, "mapper"));
        assertSame(repository, ReflectionTestUtils.getField(service, "repository"));
        assertTrue(context.getBeansOfType(CarryDemoRunner.class).isEmpty());
    }

    @Test
    void controllerRegistrationCanBeReadThroughServiceAndSameRepository() {
        controller.register(101, new EmployeeRequest("テスト開発社員", 1));
        controller.register(102, new EmployeeRequest("テスト営業社員", 2));
        assertSame(repository.findById(101).orElseThrow(), service.findById(101));
        assertEquals("テスト開発社員", service.findById(101).getName());
        assertEquals("開発部", service.findById(101).getDepartment().getName());
        assertEquals("営業部", service.findById(102).getDepartment().getName());
        assertTrue(service.findAll().stream().anyMatch(e -> e.getId() == 101));
        assertTrue(service.findByDepartmentId(1).stream().anyMatch(e -> e.getId() == 101));
        assertFalse(service.findByDepartmentId(1).stream().anyMatch(e -> e.getId() == 102));
        assertTrue(service.findByDepartmentId(999).isEmpty());
        assertEquals("開発部", mapper.toResponse(service.findById(101)).departmentName());
    }

    @Test
    void nullEmptyAndBlankNamesAreRejectedWithoutSaving() {
        for (String name : new String[] { null, "", "  " }) {
            var error = assertThrows(IllegalArgumentException.class,
                    () -> controller.register(201, new EmployeeRequest(name, 1)));
            assertEquals("社員名を入力してください。", error.getMessage());
            assertTrue(repository.findById(201).isEmpty());
        }
    }

    @Test
    void unknownDepartmentIsRejectedWithoutSaving() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> controller.register(202, new EmployeeRequest("部署不正", 999)));
        assertEquals("存在しない部署です。", error.getMessage());
        assertTrue(repository.findById(202).isEmpty());
    }

    @Test
    void unknownEmployeeKeepsExistingException() {
        var error = assertThrows(EmployeeNotFoundException.class, () -> service.findById(999));
        assertEquals("社員が見つかりません。 id=999", error.getMessage());
    }

    @Test
    void singletonSharesTheCounter() {
        EmployeeService second = context.getBean(EmployeeService.class);
        assertSame(service, second);
        int before = second.getCounter();
        service.incrementCounter();
        assertEquals(before + 1, second.getCounter());
    }
}
