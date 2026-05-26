package com.zms.bridge;

// 抽象操作系统
public abstract class OperatingSystem {

    // VideoFile变量
    protected VideoFile videoFile;

    public OperatingSystem(VideoFile videoFile) {
        this.videoFile = videoFile;
    }

    public abstract void play(String fileName);
}
