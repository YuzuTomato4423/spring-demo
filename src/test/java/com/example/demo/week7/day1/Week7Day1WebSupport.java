package com.example.demo.week7.day1;

import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.example.demo.controller.Week6Day3EmployeeController;
import com.example.demo.exception.EmployeeApiExceptionHandler;
import com.example.demo.service.EmployeeBusinessService;
import com.example.demo.service.EmployeeJpaService;

/** SPR-W7-D1-01～04: MVCと実際のValidation/Adviceを検証し、Service以下と実DBは切り離す。 */
@WebMvcTest(Week6Day3EmployeeController.class)
@Import(EmployeeApiExceptionHandler.class)
@ActiveProfiles("spr-w6-d3")
@Tag("spr-w7-d1")
abstract class Week7Day1WebSupport {
    @Autowired MockMvc mvc;
    @MockitoBean EmployeeBusinessService business;
    @MockitoBean EmployeeJpaService existing;
}
