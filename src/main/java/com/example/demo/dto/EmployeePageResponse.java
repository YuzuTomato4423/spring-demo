package com.example.demo.dto;
import java.util.List;
import org.springframework.data.domain.Page;

/** SPR-W5-D4-03: Page実装の内部構造に依存しない公開用ページ応答。 */
public record EmployeePageResponse(List<EmployeeResponse> content, int number, int size,
        long totalElements, int totalPages, boolean first, boolean last) {
    public static EmployeePageResponse from(Page<EmployeeResponse> page) {
        return new EmployeePageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
