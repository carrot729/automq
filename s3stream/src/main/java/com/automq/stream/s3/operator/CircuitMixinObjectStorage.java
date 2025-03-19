/*
 * Copyright 2024, AutoMQ HK Limited.
 *
 * The use of this file is governed by the Business Source License,
 * as detailed in the file "/LICENSE.S3Stream" included in this repository.
 *
 * As of the Change Date specified in that file, in accordance with
 * the Business Source License, use of this software will be governed
 * by the Apache License, Version 2.0
 */

package com.automq.stream.s3.operator;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CircuitMixinObjectStorage implements ObjectStorage {
    private final ObjectStorage mainObjectStorage;
    private final ObjectStorage fallbackObjectStorage;

    public CircuitMixinObjectStorage(ObjectStorage mainObjectStorage, ObjectStorage fallbackObjectStorage) {
        this.mainObjectStorage = mainObjectStorage;
        this.fallbackObjectStorage = fallbackObjectStorage;
    }

    @Override
    public boolean readinessCheck() {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public Writer writer(WriteOptions options, String objectPath) {
        // 根据开关
        return null;
    }

    @Override
    public CompletableFuture<ByteBuf> rangeRead(ReadOptions options, String objectPath, long start, long end) {
        return null;
    }

    @Override
    public CompletableFuture<WriteResult> write(WriteOptions options, String objectPath, ByteBuf buf) {
        return null;
    }

    @Override
    public CompletableFuture<List<ObjectInfo>> list(String prefix) {
        return null;
    }

    @Override
    public CompletableFuture<Void> delete(List<ObjectPath> objectPaths) {
        return null;
    }
}
