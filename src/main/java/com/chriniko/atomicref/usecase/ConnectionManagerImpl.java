package com.chriniko.atomicref.usecase;

public class ConnectionManagerImpl implements ConnectionManager {

	public void commit(WorkspaceSession workspaceSession) {
		System.out.println(workspaceSession.getSessionRef() + " --- COMMITTED SESSION!");
	}
}
