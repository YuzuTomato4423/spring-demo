package com.example.demo.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.dto.EmployeeRequest;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.model.Department;
import com.example.demo.model.Employee;

/** 受付・返却形式と業務モデルを分け、外部向けDTOへ内部構造を持ち込まない。 */
@Component
public class EmployeeMapper {

    private static final Logger log = LoggerFactory.getLogger(EmployeeMapper.class);

    public EmployeeMapper() {
        log.info("[BEAN] EmployeeMapper@{} created",
                Integer.toHexString(System.identityHashCode(this)));
    }

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment().getName()
        );
    }

    public Employee toEntity(
            int id,
            EmployeeRequest request,
            Department department) {

        return new Employee(
                id,
                request.name(),
                department
        );
    }
}
