package com.example.demo.dto;

import com.example.demo.entity.EmployeeEntity;

/** SPR-W6-D3: Transaction内で公開DTOへ変換し、Entity関連をJSONへ公開しない。 */
public record BusinessEmployeeResponse(long id,String name,String email,String departmentName) {
    public static BusinessEmployeeResponse from(EmployeeEntity employee) {
        return new BusinessEmployeeResponse(employee.getId(),employee.getName(),employee.getEmail(),employee.getDepartment().getName());
    }
}
