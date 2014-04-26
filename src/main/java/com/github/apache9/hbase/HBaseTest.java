/**
 * @(#)HBaseTest.java, 2014-4-26. 
 *
 * Copyright (c) 2014, Wandou Labs and/or its affiliates. All rights reserved.
 * WANDOU LABS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.github.apache9.hbase;

import org.apache.hadoop.hbase.HBaseTestingUtility;

/**
 * @author zhangduo
 */
public class HBaseTest {

    public static void main(String[] args) throws Exception {
        HBaseTestingUtility util = new HBaseTestingUtility();
        util.startMiniCluster(2);
    }
}
