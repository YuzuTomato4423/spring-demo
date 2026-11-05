package com.example.demo.dto;

import java.util.Locale;
import jakarta.validation.constraints.*;

/** SPR-W6-D3-01: Day3だけの入力契約。過去課題のEmployeeRequestは変更しない。 */
public record BusinessEmployeeRequest(
        @NotBlank(message="社員名を入力してください。") String name,
        @NotBlank(message="メールを入力してください。")
        @Email(message="メール形式を確認してください。")
        @Size(max=254,message="メールは254文字以内で入力してください。") String email,
        @Positive(message="部署IDは正の整数で指定してください。") int departmentId) {
    public BusinessEmployeeRequest {
        // 同じ表記のメールを一意性判定・保存・応答で共通して使用する。
        email=email==null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
