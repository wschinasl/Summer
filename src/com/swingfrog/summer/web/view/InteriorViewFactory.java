package com.swingfrog.summer.web.view;

public class InteriorViewFactory {

	public BlankView createBlankView() {
		return new BlankView();
	}
	
	public ErrorView createErrorView(int status, long code, String msg) {
		return new ErrorView(status, code, msg);
	}
	
	public ErrorView createErrorView(int status, String msg) {
		return new ErrorView(status, msg);
	}
	
}
