package com.biolab.user.dto.response;

import lombok.*;
import java.util.List;

/**
 * Generic paginated response wrapper used by all paginated endpoints.
 *
 * @param <T> the type of content items
 * @author BioLab Engineering Team
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PageResponse<T> {

    /** The page content items. */
    private List<T> content;

    /** Current page number (zero-based). */
    private int page;

    /** Page size (items per page). */
    private int size;

    /** Total number of items across all pages. */
    private long totalElements;

    /** Total number of pages. */
    private int totalPages;

    /** True if there is a next page. */
    private boolean hasNext;
}
