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

package com.automq.stream.s3;

import com.automq.stream.s3.metadata.S3ObjectMetadata;
import com.automq.stream.s3.operator.ObjectStorage;
import com.automq.stream.s3.operator.Writer;
import com.automq.stream.utils.FutureUtil;

import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;

/**
 * The circuit object storage only adapts to write ahead log uploading and block cache reading.
 */
public class CircuitObjectStorage implements ObjectStorage {
    private final ObjectStorage mainStorage;
    private final ObjectStorage fallbackStorage;
    private CircuitStatus circuitStatus = CircuitStatus.OPEN;

    public CircuitObjectStorage(ObjectStorage mainStorage, ObjectStorage fallbackStorage) {
        this.mainStorage = mainStorage;
        this.fallbackStorage = fallbackStorage;
    }

    @Override
    public boolean readinessCheck() {
        return true;
    }

    @Override
    public void close() {
    }

    @Override
    public Writer writer(WriteOptions options, String objectPath) {
        switch (circuitStatus) {
            case OPEN:
            case HALF_OPEN:
                return new CircuitWriter(options, objectPath);
            case CLOSED:
                return fallbackStorage.writer(options, objectPath);
            default:
                throw new IllegalStateException("Unknown circuit status " + circuitStatus);
        }
    }

    @Override
    public CompletableFuture<ByteBuf> rangeRead(ReadOptions options, String objectPath, long start, long end) {
        if (options.bucket() == mainStorage.bucketId()) {
            return mainStorage.rangeRead(options, objectPath, start, end);
        } else if (options.bucket() == fallbackStorage.bucketId()) {
            return fallbackStorage.rangeRead(options, objectPath, start, end);
        } else {
            return FutureUtil.failedFuture(new IllegalArgumentException(String.format("Cannot match bucket id in (%s, %s)",
                mainStorage.bucketId(), fallbackStorage.bucketId())));
        }
    }

    @Override
    public CompletableFuture<WriteResult> write(WriteOptions options, String objectPath, ByteBuf buf) {
        return FutureUtil.failedFuture(new UnsupportedOperationException());
    }

    @Override
    public CompletableFuture<List<ObjectInfo>> list(String prefix) {
        return FutureUtil.failedFuture(new UnsupportedOperationException());
    }

    @Override
    public CompletableFuture<Void> delete(List<ObjectPath> objectPaths) {
        return FutureUtil.failedFuture(new UnsupportedOperationException());
    }

    @Override
    public short bucketId() {
        throw new UnsupportedOperationException();
    }

    public synchronized CompletableFuture<Void> transitionTo(CircuitStatus to) {
        // TODO: async the operation
        CircuitStatus from = circuitStatus;
        if (to == from) {
            return CompletableFuture.completedFuture(null);
        } else {
            if (from == CircuitStatus.OPEN && to == CircuitStatus.CLOSED) {
                // The circuit status transition from OPEN to CLOSED
                // TODO: register node with mark
            }
        }
        // 请求远程
        // 先用阻塞实现
        return CompletableFuture.completedFuture(null);
    }

    public class CircuitWriter implements Writer {
        private final WriteOptions options;
        private final String objectPath;
        private final List<Pair<ByteBuf, CompletableFuture<Void>>> writeList = new LinkedList<>();
        private Writer writer;

        public CircuitWriter(WriteOptions options, String objectPath) {
            this.options = options;
            this.objectPath = objectPath;
            this.writer = mainStorage.writer(options.copy().timeout(TimeUnit.SECONDS.toMillis(30)), objectPath);
        }

        @Override
        public synchronized CompletableFuture<Void> write(ByteBuf data) {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            data = data.retain();
            ByteBuf finalData = data;
            cf.whenComplete((nil, ex) -> finalData.release());
            writeList.add(Pair.of(data.retain(), cf));
            writer.write(data);
            return cf;
        }

        @Override
        public void copyOnWrite() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void copyWrite(S3ObjectMetadata s3ObjectMetadata, long start, long end) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasBatchingPart() {
            return false;
        }

        @Override
        public CompletableFuture<Void> close() {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            writer.close().whenComplete((rst, ex) -> {
                if (ex == null) {
                    writeList.forEach(p -> p.getValue().complete(null));
                    cf.complete(null);
                    return;
                }
                transitionTo(CircuitStatus.CLOSED).whenComplete((nil, ex2) -> {
                    writer = fallbackStorage.writer(options.copy(), objectPath);
                    writeList.forEach(p -> FutureUtil.propagate(writer.write(p.getKey()), p.getValue()));
                    FutureUtil.propagate(writer.close(), cf);
                });
            });
            return cf;
        }

        @Override
        public CompletableFuture<Void> release() {
            return writer.release().thenAccept(ignore -> writeList.forEach(p -> p.getValue().complete(null)));
        }

        @Override
        public short bucketId() {
            return writer.bucketId();
        }
    }

    public enum CircuitStatus {
        OPEN, HALF_OPEN, CLOSED
    }
}
