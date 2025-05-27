package org.kosa.userservice.dto;

import org.springframework.data.domain.Sort;

import java.util.Set;

public class SortUtils {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "userid", "loginTime");

    public static Sort getSort(String sortBy) {
        if (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) {
            return Sort.by(Sort.Direction.ASC, sortBy);
        }
        return Sort.unsorted();
    }
}