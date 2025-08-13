package com.zms.builder.demo2;

public class Client {
    public static void main(String[] args) {
        Phone phone = new Phone.Builder()
                .cpu("intel")
                .mainBoard("华硕")
                .memory("金士顿")
                .build();

        System.out.println(phone);
    }
}
