package com.biolab.auth.dto.response;

import lombok.*;
import java.util.List;

/** Generic paginated response wrapper. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}
