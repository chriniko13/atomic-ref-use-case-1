package com.chriniko.atomicref.usecase;

public class WorkspaceSession {

	private final String sessionRef;

	public WorkspaceSession(String procId) {
		sessionRef = System.currentTimeMillis() + "__" + Thread.currentThread().getName() + "__" + procId;
	}

	public String getSessionRef() {
		return sessionRef;
	}
}
