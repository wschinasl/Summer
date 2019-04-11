package com.test.summerDemo.game.remote;

import com.swingfrog.summer.app.Summer;

public class RemoteGameServer {

	public static ChatRemote getChatRemote() {
		return Summer.getRandomRemoteInvokeObject("Game", ChatRemote.class);
	}
	
}
