package com.swingfrog.summer.server;

import com.swingfrog.summer.protocol.SessionRequest;

public interface SessionHandler {

	public boolean accpet(SessionContext ctx);
	
	public void added(SessionContext ctx);
	
	public void removed(SessionContext ctx);
	
	public boolean receive(SessionContext ctx, SessionRequest request);
	
	public void heartTimeOut(SessionContext ctx);
	
	public void sendTooFastMsg(SessionContext ctx);
	
	public void lengthTooLongMsg(SessionContext ctx);
	
	public void unableParseMsg(SessionContext ctx);
	
	public void repetitionMsg(SessionContext ctx);
	
}
