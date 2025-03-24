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

import com.automq.stream.s3.metrics.operations.S3Operation;
import com.automq.stream.s3.network.NetworkBandwidthLimiter;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;

public class LocalFileObjectStorage extends AbstractObjectStorage {
    protected LocalFileObjectStorage(BucketURI bucketURI,
        NetworkBandwidthLimiter networkInboundBandwidthLimiter,
        NetworkBandwidthLimiter networkOutboundBandwidthLimiter,
        int maxObjectStorageConcurrency, int currentIndex, boolean readWriteIsolate, boolean checkS3ApiMode,
        boolean manualMergeRead, String threadPrefix) {
        super(bucketURI, networkInboundBandwidthLimiter, networkOutboundBandwidthLimiter, maxObjectStorageConcurrency, currentIndex, readWriteIsolate, checkS3ApiMode, manualMergeRead, threadPrefix);
    }

    @Override
    CompletableFuture<ByteBuf> doRangeRead(ReadOptions options, String path, long start, long end) {
        return null;
    }

    @Override
    CompletableFuture<Void> doWrite(WriteOptions options, String path, ByteBuf data) {
        return null;
    }

    @Override
    CompletableFuture<String> doCreateMultipartUpload(WriteOptions options, String path) {
        return null;
    }

    @Override
    CompletableFuture<ObjectStorageCompletedPart> doUploadPart(WriteOptions options, String path, String uploadId,
        int partNumber, ByteBuf part) {
        return null;
    }

    @Override
    CompletableFuture<ObjectStorageCompletedPart> doUploadPartCopy(WriteOptions options, String sourcePath, String path,
        long start, long end, String uploadId, int partNumber) {
        return null;
    }

    @Override
    CompletableFuture<Void> doCompleteMultipartUpload(WriteOptions options, String path, String uploadId,
        List<ObjectStorageCompletedPart> parts) {
        return null;
    }

    @Override
    CompletableFuture<Void> doDeleteObjects(List<String> objectKeys) {
        return null;
    }

    @Override
    Pair<RetryStrategy, Throwable> toRetryStrategyAndCause(Throwable ex, S3Operation operation) {
        return null;
    }

    @Override
    void doClose() {

    }

    @Override
    CompletableFuture<List<ObjectInfo>> doList(String prefix) {
        return null;
    }

    @Override
    public boolean readinessCheck() {
        return false;
    }
}
