package com.example.demo.dto;

import java.util.List;
import jakarta.validation.constraints.*;

/** SPR-W6-D3-03: 入力順に社員を異動。社員ID重複はServiceで拒否する。 */
public record EmployeeTransferRequest(
        @NotEmpty(message="社員IDを1件以上指定してください。")
        List<@NotNull(message="社員IDは必須です。") @Positive(message="社員IDは正の整数で指定してください。") Long> employeeIds,
        @Positive(message="部署IDは正の整数で指定してください。") int departmentId) { }
