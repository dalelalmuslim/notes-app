package com.example.notely.data;

/**
 * Delivers the result of an asynchronous repository/update operation.
 * Callbacks are always invoked on the main thread.
 */
public interface ResultCallback<T> {
    void onResult(T result);
}
