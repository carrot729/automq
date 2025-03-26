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

import com.automq.stream.s3.exceptions.ObjectNotExistException;
import com.automq.stream.s3.metadata.ObjectUtils;
import com.automq.stream.utils.FutureUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("S3Unit")
public class LocalFileObjectStorageTest {

    LocalFileObjectStorage objectStorage;

    @BeforeEach
    public void setup() {
        objectStorage = new LocalFileObjectStorage(BucketURI.parse("0@file:///tmp/automq_test/localfilestoragetest"));
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        String key = ObjectUtils.genKey(0, 100);
        Writer writer = objectStorage.writer(new ObjectStorage.WriteOptions(), key);
        writer.write(Unpooled.wrappedBuffer("hello ".getBytes(StandardCharsets.UTF_8)));
        writer.write(Unpooled.wrappedBuffer("world".getBytes(StandardCharsets.UTF_8)));
        writer.close().get();

        ByteBuf buf = objectStorage.rangeRead(new ObjectStorage.ReadOptions(), key, 0, -1L).get();
        Assertions.assertEquals("hello world", substr(buf, 0, buf.readableBytes()));
        Assertions.assertEquals("hello", substr(buf, 0, 5));

        objectStorage.delete(List.of(new ObjectStorage.ObjectInfo(objectStorage.bucketId(), key, 0, 0))).get();

        Throwable exception = null;
        try {
            objectStorage.rangeRead(new ObjectStorage.ReadOptions(), key, 0, -1L).get();
        } catch (Throwable e) {
            exception = FutureUtil.cause(e);
        }
        Assertions.assertEquals(ObjectNotExistException.class, Optional.ofNullable(exception).map(Throwable::getClass).orElse(null));
    }

    private String substr(ByteBuf buf, int start, int end) {
        buf = buf.duplicate();
        byte[] bytes = new byte[end - start];
        buf.skipBytes(start);
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
