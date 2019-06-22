package com.swingfrog.summer.db;

public abstract class AbstractPersistenceEntity {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "AbstractPersistenceEntity{" +
                "id=" + id +
                '}';
    }
}
