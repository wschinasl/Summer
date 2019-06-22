package com.swingfrog.summer.db;

import com.swingfrog.summer.ioc.ContainerMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceMgr {

    private static final Logger log = LoggerFactory.getLogger(PersistenceMgr.class);

    private static class SingleCase {
        public static final PersistenceMgr INSTANCE = new PersistenceMgr();
    }

    private PersistenceMgr() {

    }

    public static PersistenceMgr get() {
        return SingleCase.INSTANCE;
    }

    public void createTable() {
        ContainerMgr.get().listDeclaredComponent(PersistenceDao.class).forEach(p -> {
            if (p.createTable()) {
                log.info("create table {} success", p.getTableName());
            } else {
                log.info("table {} exists", p.getTableName());
            }
        });
    }
}
