package com.test.summerDemo.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.swingfrog.summer.app.Summer;
import com.swingfrog.summer.app.SummerApp;

public class GameApp implements SummerApp{

	private static final Logger log = LoggerFactory.getLogger(GameApp.class);
	
	@Override
	public void init() {
		log.info("game init");
	}

	@Override
	public void start() {
		log.info("game start");
	}

	@Override
	public void stop() {
		log.info("game stop");
	}
	
	public static void main(String[] args) throws Exception {
		Summer.hot(new GameApp(), 
				GameApp.class.getPackage().getName(), 
				"lib", 
				"config/game/server.properties", 
				"config/log.properties", 
				"config/redis.properties", 
				"config/db.properties", 
				"config/task.properties");
	}

}
