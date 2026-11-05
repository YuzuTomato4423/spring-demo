package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.EmployeeRequest;
import com.example.demo.mapper.EmployeeMapper;
import com.example.demo.model.Department;
import com.example.demo.model.Employee;
import com.example.demo.service.EmployeeService;

/** 受付と部署の解決を担い、登録ルール・通知はServiceへ委ねる。 */
@RestController
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService service;
    private final EmployeeMapper mapper;

    public EmployeeController(
            EmployeeService service,
            EmployeeMapper mapper) {

        this.service = service;
        this.mapper = mapper;
        // 学習用のDI照合に使う。社員や設定値はこのログへ含めない。
        log.info("[DI] EmployeeController@{} -> EmployeeService@{}, EmployeeMapper@{}",
                Integer.toHexString(System.identityHashCode(this)),
                Integer.toHexString(System.identityHashCode(service)),
                Integer.toHexString(System.identityHashCode(mapper)));
    }

    /** 受付データは登録ごとに生成し、singletonの受付Beanに社員情報を保持しない。 */
    public void register(int id, EmployeeRequest request) {

        Department department;

        if (request.departmentId() == 1) {
            department = new Department(1, "開発部");
        } else if (request.departmentId() == 2) {
            department = new Department(2, "営業部");
        } else {
            throw new IllegalArgumentException("存在しない部署です。");
        }

        Employee employee =
                mapper.toEntity(id, request, department);

        service.register(employee);
    }
}
