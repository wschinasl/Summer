package com.test.summerDemo.gate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.swingfrog.summer.app.Summer;
import com.swingfrog.summer.app.SummerApp;

public class GateApp implements SummerApp{

	private static final Logger log = LoggerFactory.getLogger(GateApp.class);
	
	@Override
	public void init() {
		log.info("gate init");
	}

	@Override
	public void start() {
		log.info("gate start");
	}

	@Override
	public void stop() {
		log.info("gate stop");
	}
	
	public static void main(String[] args) throws Exception {
		Summer.hot(new GateApp(), 
				GateApp.class.getPackage().getName(), 
				"lib", 
				"config/gate/server.properties", 
				"config/log.properties", 
				"config/redis.properties", 
				"config/db.properties", 
				"config/task.properties");
	}

}
