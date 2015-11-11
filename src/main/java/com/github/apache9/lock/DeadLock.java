package com.github.apache9.lock;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Apache9
 */
public class DeadLock {

    private static void deadLock(Lock lock1, Lock lock2, CyclicBarrier barrier) {
        lock1.lock();
        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Dead Lock");
        lock2.lock();
    }

    public static void main(String[] args) throws InterruptedException {
        Lock lock1 = new ReentrantLock();
        Lock lock2 = new ReentrantLock();
        CyclicBarrier barrier = new CyclicBarrier(2);
        new Thread(() -> deadLock(lock1, lock2, barrier)).start();
        new Thread(() -> deadLock(lock2, lock1, barrier)).start();
    }
}
