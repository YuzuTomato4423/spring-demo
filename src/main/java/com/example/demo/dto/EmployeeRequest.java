package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * SPR-W4-D3-01: JSON受付専用DTO。POSTのIDは採番、PUTではURLで指定。
 * SPR-W4-D4-01: 入力制約をDTOへ宣言し、POST/PUTで同じルールを再利用する。
 * Controllerの手書きifを増やさず@Validで検証する。部署の存在などの業務ルールはServiceへ委ねる。
 */
public record EmployeeRequest(
        @NotBlank(message = "社員名を入力してください。") String name,
        @Positive(message = "部署IDは正の整数で指定してください。") int departmentId
) {
}
