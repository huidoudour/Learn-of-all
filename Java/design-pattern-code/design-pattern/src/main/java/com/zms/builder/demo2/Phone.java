package com.zms.builder.demo2;

// 手机类
public class Phone {
    private String cpu;
    private String memory;
    private String mainBoard;

    // 私有构造方法
    private Phone(Builder builder) {
        this.cpu = builder.cpu;
        this.memory = builder.memory;
        this.mainBoard = builder.mainBoard;
    }

    @Override
    public String toString() {
        return "Phone{" +
                "cpu='" + cpu + '\'' +
                ", memory='" + memory + '\'' +
                ", mainBoard='" + mainBoard + '\'' +
                '}';
    }

    // 静态内部类
    public static final class Builder {
        private String cpu;
        private String memory;
        private String mainBoard;

        public Builder cpu(String cpu) {
            this.cpu = cpu;
            return this;
        }
        public Builder memory(String memory) {
            this.memory = memory;
            return this;
        }
        public Builder mainBoard(String mainBoard) {
            this.mainBoard = mainBoard;
            return this;
        }

        // 使用构建者创建Phone对象
        public Phone build() {
            return new Phone(this);
        }
    }
}
