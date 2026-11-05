package com.example.demo.dto;
import java.util.List;
/** SPR-W6-D3-03: 全件成功した場合だけ返す結果。部分成功は返さない。 */
public record EmployeeTransferResponse(int updatedCount,List<BusinessEmployeeResponse> employees) { }
