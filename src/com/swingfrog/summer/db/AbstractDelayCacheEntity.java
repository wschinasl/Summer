package com.swingfrog.summer.db;

public abstract class AbstractDelayCacheEntity extends AbstractPersistenceEntity {

    private volatile Long lastModifyTime;

    Long getLastModifyTime() {
        return lastModifyTime;
    }

    void setLastModifyTime(Long lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }

    @Override
    public String toString() {
        return "AbstractDelayCacheEntity{" +
                "lastModifyTime=" + lastModifyTime +
                "} " + super.toString();
    }

}
