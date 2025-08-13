package com.zms.bridge;

// Rmvb视频文件
public class RmvbFile implements VideoFile{
    @Override
    public void decode(String fileName) {
        System.out.println("Rmbv 视频文件：" + fileName);
    }
}
