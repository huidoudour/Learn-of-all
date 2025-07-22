package com.zms.cas;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class TestAtomicMarkableReference {
    public static void main(String[] args) {
        // 创建一个 AtomicMarkableReference 实例，初始值为 "Hello" 和标记 false
        AtomicMarkableReference<String> atomicRef = new AtomicMarkableReference<>("Hello", false);

        // 客户端调用：尝试更新引用和标记
        boolean updated = atomicRef.compareAndSet("Hello", "World", false, true);
        System.out.println("Updated: " + updated); // 输出：Updated: true

        // 获取当前值和标记
        String currentValue = atomicRef.getReference();
        boolean currentMark = atomicRef.isMarked();
        System.out.println("Current Value: " + currentValue + ", Mark: " + currentMark); // 输出：Current Value: World, Mark: true

        // 尝试基于旧值和旧标记更新，但这次会失败，因为当前值或标记与预期的不匹配
        updated = atomicRef.compareAndSet("Hello", "Java", false, false);
        System.out.println("Updated: " + updated); // 输出：Updated: false
    }
}

// class GarbageBag {
//     private String desc;
//
//     public GarbageBag(String desc) {
//         this.desc = desc;
//     }
//     public String getDesc() {
//         return desc;
//     }
//
//     @Override
//     public String toString() {
//         return "GarbageBag{" +
//                 "desc='" + desc + '\'' +
//                 '}';
//     }
// }
