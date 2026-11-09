package com.example.demo.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.dto.EmployeeResponse;
import com.example.demo.model.Employee;

/** SPR-W8-D1-02: 公開DTOへの変換を担当し、部署オブジェクトなどの内部構造を応答へ持ち込まない。 */
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

}
