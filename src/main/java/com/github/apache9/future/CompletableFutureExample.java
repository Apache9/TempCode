package com.github.apache9.future;

import java.util.concurrent.CompletableFuture;

/**
 * @author Apache9
 */
public class CompletableFutureExample {

    public static void main(String[] args) {
        CompletableFuture<Integer> c = new CompletableFuture<>();
        c.thenRun(() -> System.out.println("Normal")).handle((x, e) -> {
            if (e != null) {
                System.out.println("exception is propagated");
                e.printStackTrace(System.out);
            } else {
                System.out.println("exception is not propagated");
            }
            return x;
        });
        c.completeExceptionally(new Exception("test"));
    }
}
