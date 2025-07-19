package com.zms.prototype.demo2;

public class Certificate implements Cloneable {
    // 获奖学生
    private Student student;

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }


    @Override
    public String toString() {
        return "Certificate{" +
                "student=" + student +
                '}';
    }

    @Override
    public Certificate clone() {
        try {
            Certificate clone = (Certificate) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
