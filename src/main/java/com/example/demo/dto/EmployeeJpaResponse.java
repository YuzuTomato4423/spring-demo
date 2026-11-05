package com.example.demo.dto;

/** SPR-W5-D2-03: 公開項目は従来どおり。JPAのLong IDを切り詰めず返す。 */
public record EmployeeJpaResponse(Long id, String name, String departmentName) { }
