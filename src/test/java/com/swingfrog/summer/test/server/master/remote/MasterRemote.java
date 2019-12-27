package com.swingfrog.summer.test.server.master.remote;

import com.google.common.collect.ImmutableMap;
import com.swingfrog.summer.annotation.Remote;
import com.swingfrog.summer.annotation.SessionQueue;
import com.swingfrog.summer.app.Summer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Remote
public class MasterRemote {

    @SessionQueue
    public int add(int a, int b) {
        log.info("recv from slave a[{}] b[{}]", a, b);
        Summer.getServerPush().asyncPushToClusterRandomServer("Slave", "SlavePush", "recv", ImmutableMap.of("msg", "hello"));
        return a + b;
    }

}
