package com.zms.bridge;

public class Client {
    public static void main(String[] args) {
        OperatingSystem system = new Mac(new AviFile());
        system.play("战狼2");
    }
}
