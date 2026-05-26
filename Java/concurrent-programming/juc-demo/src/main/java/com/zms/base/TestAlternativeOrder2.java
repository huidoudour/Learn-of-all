package com.zms.base;



// 使用wait、notify实现交替打印
public class TestAlternativeOrder2 {
    public static void main(String[] args) {
        AlternativeOrder2 order2 = new AlternativeOrder2("a", 5);
        order2.print("a", "b");
        order2.print("b", "c");
        order2.print("c", "a");
    }
}
class AlternativeOrder2 {
    // 等待标记
    private String flag;
    // 循环次数
    private int loopNumber;

    public AlternativeOrder2(String flag, int loopNumber) {
        this.flag = flag;
        this.loopNumber = loopNumber;
    }

    public void print(String flag, String nextFlag) {
        for (int i = 0; i < loopNumber; i++) {
            synchronized (this) {
                while(!this.flag.equals(nextFlag)) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                this.flag = nextFlag;
                this.notifyAll();
            }
        }
    }
}
