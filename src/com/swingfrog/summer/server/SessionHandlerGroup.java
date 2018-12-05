package com.swingfrog.summer.server;

import java.util.ArrayList;
import java.util.List;

import com.swingfrog.summer.protocol.SessionRequest;

public class SessionHandlerGroup implements SessionHandler {

	private List<SessionHandler> sessionHandlerList;
	
	public SessionHandlerGroup() {
		sessionHandlerList = new ArrayList<>();
	}
	
	public void addSessionHandler(SessionHandler sessionHandler) {
		sessionHandlerList.add(sessionHandler);
	}
	
	public void removeSessionHandler(SessionHandler sessionHandler) {
		sessionHandlerList.remove(sessionHandler);
	}

	@Override
	public boolean accpet(SessionContext ctx) {
		for (int i = 0; i < sessionHandlerList.size(); i ++) {
			if (!sessionHandlerList.get(i).accpet(ctx)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void added(SessionContext ctx) {
		for (int i = 0; i < sessionHandlerList.size(); i ++) {
			sessionHandlerList.get(i).added(ctx);
		}
	}

	@Override
	public void removed(SessionContext ctx) {
		for (int i = 0; i < sessionHandlerList.size(); i ++) {
			sessionHandlerList.get(i).removed(ctx);
		}
	}

	@Override
	public boolean receive(SessionContext ctx, SessionRequest request) {
		for (int i = 0; i < sessionHandlerList.size(); i ++) {
			if (!sessionHandlerList.get(i).receive(ctx, request)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void heartTimeOut(SessionContext ctx) {
		for (int i = 0; i < sessionHandlerList.size(); i ++) {
			sessionHandlerList.get(i).heartTimeOut(ctx);
		}
	}

	@Override
	public void sendTooFastMsg(SessionContext ctx) {
		for (int i = 0; i < sessionHandlerList.size(); i ++) {
			sessionHandlerList.get(i).sendTooFastMsg(ctx);
		}
	}

	@Override
	public void lengthTooLongMsg(SessionContext ctx) {
		for (int i = 0; i < sessionHandlerList.size(); i ++) {
			sessionHandlerList.get(i).lengthTooLongMsg(ctx);
		}
	}

	@Override
	public void unableParseMsg(SessionContext ctx) {
		for (int i = 0; i < sessionHandlerList.size(); i ++) {
			sessionHandlerList.get(i).unableParseMsg(ctx);
		}
	}

	@Override
	public void repetitionMsg(SessionContext ctx) {
		for (int i = 0; i < sessionHandlerList.size(); i ++) {
			sessionHandlerList.get(i).repetitionMsg(ctx);
		}
	}
	
}
