package com.zms.bridge;

// avi视频文件
public class AviFile implements VideoFile{
    @Override
    public void decode(String fileName) {
        System.out.println("avi 视频文件：" + fileName);
    }
}
