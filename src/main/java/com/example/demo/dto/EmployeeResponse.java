package com.example.demo.dto;
/**
 * SPR-W4-D2-04 / SPR-W5-D3-03: 公開項目を限定し、Entityや部署オブジェクトを返さない。
 * JPAのLong IDを切り詰めないようlongを使う。従来APIのJSON項目名は同じ。
 */
public record EmployeeResponse(long id, String name, String departmentName) { }
