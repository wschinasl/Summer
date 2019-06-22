package com.swingfrog.summer.db;

public class DelayCacheConfig {

    private int coreThread;

    public int getCoreThread() {
        return coreThread;
    }

    public void setCoreThread(int coreThread) {
        this.coreThread = coreThread;
    }

    @Override
    public String toString() {
        return "DelayCacheConfig{" +
                "coreThread=" + coreThread +
                '}';
    }
}
