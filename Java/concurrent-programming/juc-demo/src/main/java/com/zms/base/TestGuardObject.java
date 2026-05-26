package com.zms.base;

import lombok.extern.slf4j.Slf4j;

// 测试保护性暂停
@Slf4j
public class TestGuardObject {
    public static void main(String[] args) {
        GuardObject guardObject = new GuardObject();

        new Thread(() -> {
            String response = (String)guardObject.getResponse();
            log.debug("response: {}", response);
        }, "t1").start();


        new Thread(() -> {
            guardObject.setResponse("2233");
        }, "t2").start();
    }
}

class GuardObject {
    private Object response;

    // 获取结果
    public Object getResponse() {
        synchronized (this) {
            while (response == null) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return response;
    }

    // 设置结果
    public void setResponse(Object response) {
        synchronized (this) {
            this.response = response;
            this.notifyAll();
        }
    }
}
