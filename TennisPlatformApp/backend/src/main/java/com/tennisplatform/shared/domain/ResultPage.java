package com.tennisplatform.shared.domain;

import java.util.List;
import java.util.function.Function;

/** One page of a listing, in the shape 11-contrato-api.md gives every listing. */
public record ResultPage<T>(List<T> items, int page, int size, long totalItems) {

    /** Copied on the way in: a record hands out its list by reference. */
    public ResultPage {
        items = List.copyOf(items);
    }

    public <R> ResultPage<R> map(Function<T, R> mapper) {
        return new ResultPage<>(items.stream().map(mapper).toList(), page, size, totalItems);
    }
}
