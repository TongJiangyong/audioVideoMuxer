package com.serenegiant.connector;

public interface SinkConnector<T> {
    int onDataAvailable(T data);
}
