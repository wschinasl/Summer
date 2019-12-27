package com.swingfrog.summer.test.server.slave.task;

import com.swingfrog.summer.annotation.IntervalTask;
import com.swingfrog.summer.annotation.Task;
import com.swingfrog.summer.app.Summer;
import com.swingfrog.summer.test.server.master.remote.MasterRemote;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Task
public class SlaveTask {

    @IntervalTask(3000)
    public void sendToMaster() {
        MasterRemote masterRemote = Summer.getRandomRemoteInvokeObjectWithRetry("Master", MasterRemote.class);
        log.info("result from master -> {}", masterRemote.add(1, 2));
    }

}
