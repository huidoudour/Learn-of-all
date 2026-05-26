package com.zms.prototype.demo2;

public class Client {
    public static void main(String[] args) {
        Certificate certificate1 = new Certificate();
        certificate1.setStudent( new Student("老大", 18));
        Certificate certificate2 = certificate1.clone();
        certificate2.setStudent(new Student("老詹", 18));

        System.out.println(certificate1);
        System.out.println(certificate2);
    }
}
