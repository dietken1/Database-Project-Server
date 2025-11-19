package backend.databaseproject.domain.store.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 카테고리 응답 DTO
 */
@Schema(description = "상품 카테고리 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    @Schema(description = "카테고리명", example = "음료")
    private String category;

    /**
     * 카테고리 문자열을 CategoryResponse로 변환
     *
     * @param category 카테고리명
     * @return CategoryResponse
     */
    public static CategoryResponse of(String category) {
        return CategoryResponse.builder()
                .category(category)
                .build();
    }

    /**
     * 카테고리 문자열을 CategoryResponse로 변환
     *
     * @param category 카테고리명
     * @return CategoryResponse
     */
    public static CategoryResponse from(String category) {
        return CategoryResponse.builder()
                .category(category)
                .build();
    }
}
