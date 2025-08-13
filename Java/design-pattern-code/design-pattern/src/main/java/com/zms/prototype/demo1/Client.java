package com.zms.prototype.demo1;

public class Client {
    public static void main(String[] args) {
        Certificate certificate1 = new Certificate();
        certificate1.setName("zhangsan");

        Certificate certificate2 = certificate1.clone();
        // certificate2的name修改时，由于String是不可变的，所以又创建了一个新的字符串
        certificate2.setName("laozhan");

        certificate1.show();
        certificate2.show();
    }
}
