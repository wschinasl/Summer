package com.test.summerDemo.gate.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.swingfrog.summer.annotation.Push;

@Push
public class ChatPush {
	
	private static final Logger log = LoggerFactory.getLogger(ChatPush.class);

	public void recv(String msg) {
		log.info("ChatPush.recv msg[{}]", msg);
	}
	
}
