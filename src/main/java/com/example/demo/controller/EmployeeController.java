package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.EmployeeRequest;
import com.example.demo.service.EmployeeService;

/** SPR-W8-D1-01: 受付と業務判断を分け、別の入口でも同じ登録ルールを使えるようにする。 */
@RestController
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
        // 学習用のDI照合に使う。社員や設定値はこのログへ含めない。
        log.info("[DI] EmployeeController@{} -> EmployeeService@{}",
                Integer.toHexString(System.identityHashCode(this)),
                Integer.toHexString(System.identityHashCode(service)));
    }

    /** 受付データは登録ごとに生成し、singletonの受付Beanに社員情報を保持しない。 */
    public void register(int id, EmployeeRequest request) {
        service.register(id, request.name(), request.departmentId());
    }
}
