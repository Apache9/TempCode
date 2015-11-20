
package com.github.apache9.lambda;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * @author Apache9
 */
public class LambdaWithException {

    public static void noLambda(String file, List<Integer> list) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            for (Integer i: list) {
                out.writeInt(i);
            }
        }
    }

    public static void lambda(String file, List<Integer> list) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            list.forEach(i -> {
                try {
                    out.writeInt(i);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

}
