package com.test.summerDemo.gate.task;

import com.swingfrog.summer.annotation.IntervalTask;
import com.swingfrog.summer.annotation.Task;
import com.test.summerDemo.game.remote.RemoteGameServer;

@Task
public class ChatTask {

	@IntervalTask(2000)
	public void chatToGame() {
		RemoteGameServer.getChatRemote().send("hello! game server.");
	}
	
}
