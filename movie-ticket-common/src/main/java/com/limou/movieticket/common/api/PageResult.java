package com.limou.movieticket.common.api;

import java.util.List;

public record PageResult<T>(List<T> records, long total, long page, long pageSize) {
    public PageResult {
        records = List.copyOf(records);
    }
}
