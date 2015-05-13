/**
 * @(#)Rename.java, 2015-1-25. 
 *
 * Copyright (c) 2015, Wandou Labs and/or its affiliates. All rights reserved.
 * WANDOU LABS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.github.apache9.file;

import java.io.File;
import java.io.FileFilter;

/**
 * @author zhangduo
 */
public class Rename {

    public static void main(String[] args) {
        File dir = new File("\\\\192.168.0.199\\Second\\Cartoon\\One Piece");
        for (File subDir: dir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && pathname.getName().startsWith("[");
            }
        })) {
            for (File file: subDir.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    return !pathname.getName().endsWith(".torrent");
                }
            })) {
                System.out.println(file.getName());
                file.renameTo(new File(dir, file.getName()));
            }
        }
    }
}
