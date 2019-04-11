package com.test.summerDemo.game.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.swingfrog.summer.annotation.Remote;
import com.swingfrog.summer.app.Summer;

@Remote
public class ChatRemote {
	
	private static final Logger log = LoggerFactory.getLogger(ChatRemote.class);

	public void send(String msg) {
		log.info("ChatRemote.send msg[{}]", msg);
		Summer.getServerPush().asyncPushToClusterAllServer("Gate", "ChatPush", "recv", "Hi! how are you?");
	}
	
}
