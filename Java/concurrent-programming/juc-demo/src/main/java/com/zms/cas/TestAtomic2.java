package com.zms.cas;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class TestAtomic2 {
    public static void main(String[] args) {
        AtomicReference<BigDecimal> balance = new AtomicReference<>(new BigDecimal("100.00"));
        while(true) {
            BigDecimal prev = balance.get();
            BigDecimal next = prev.subtract(new BigDecimal("50.00"));
            System.out.println("循环了一次");
            if(balance.compareAndSet(prev, next)) {
                break;
            }
        }
    }
}
