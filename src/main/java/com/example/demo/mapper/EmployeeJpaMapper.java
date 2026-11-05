package com.example.demo.mapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.entity.EmployeeEntity;

@Component
@Profile("spr-w5-d2")
public class EmployeeJpaMapper {
    /**
     * SPR-W5-D3-03: 公開DTOへ部署名だけを移し、Entity関連をJSONへ直接公開しない。
     * SPR-W5-D5-03: ServiceのTransaction内で関連部署を読み、id/name/departmentNameに限定する。
     * 公開モデルを永続化モデルから分離し、将来の双方向関連による循環参照も避ける。
     * Serviceのトランザクション内で変換し、OSIVに依存しない。
     */
    public EmployeeResponse toResponse(EmployeeEntity entity) {
        return new EmployeeResponse(entity.getId(), entity.getName(), entity.getDepartment().getName());
    }
}
