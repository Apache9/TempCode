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
package com.github.apache9.posi;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Shoot {

    private static int shoot() {
        double[] rate = { 0.3d, 0.5d, 10.0d };
        boolean[] dead = new boolean[3];
        int numDeath = 0;
        outer: for (;;) {
            for (int i = 0; i < 3; i++) {
                if (dead[i]) {
                    continue;
                }
                if (ThreadLocalRandom.current().nextDouble() <= rate[i]) {
                    for (int j = 2; j >= 0; j--) {
                        if (j != i && !dead[j]) {
                            dead[j] = true;
                            if (++numDeath == 2) {
                                break outer;
                            }
                            break;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            if (!dead[i]) {
                return i;
            }
        }
        throw new IllegalStateException();
    }

    public static void main(String[] args) {
        int[] count = new int[3];
        for (int i = 0; i < 10000000; i++) {
            count[shoot()]++;
        }
        System.out.println(Arrays.toString(count));
    }
}
