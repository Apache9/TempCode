/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.apache9.jmh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.hadoop.hbase.util.Bytes;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@Measurement(iterations = 10, batchSize = 100000)
public class MapTests {

    private static final Object VAL = new Object();

    private static final List<byte[]> KEYS;

    static {
        int numKeys = 16384;
        KEYS = new ArrayList<>(numKeys);
        Random rand = new Random(123);
        for (int i = 0; i < numKeys; i++) {
            byte[] b = new byte[16];
            rand.nextBytes(b);
            KEYS.add(b);
        }
    }

    // private static final ConcurrentSkipListMap<byte[], Object> MAP = new ConcurrentSkipListMap<>(
    // new Comparator<byte[]>() {
    //
    // @Override
    // public int compare(byte[] o1, byte[] o2) {
    // return Bytes.compareTo(o1, o2);
    // }
    // });

    private static final ConcurrentHashMap<BytesWrapper, Object> MAP = new ConcurrentHashMap<>();

    private static final class BytesWrapper {
        private final byte[] b;

        public BytesWrapper(byte[] b) {
            this.b = b;
        }

        @Override
        public int hashCode() {
            return Bytes.hashCode(b);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BytesWrapper) {
                return Bytes.equals(b, ((BytesWrapper) obj).b);
            } else {
                return false;
            }
        }

    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Threads(10)
    public void test(Blackhole bh) {
        for (byte[] key : KEYS) {
            MAP.put(new BytesWrapper(key), VAL);
        }
        for (byte[] key : KEYS) {
            bh.consume(MAP.get(new BytesWrapper(key)));
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(MapTests.class.getName()).forks(1).build();
        new Runner(opt).run();
    }
}
