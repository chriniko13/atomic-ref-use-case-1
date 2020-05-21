package com.chriniko.atomicref.usecase;


/*
	Note: workspace should be owned only from one thread during execution.
 */
public interface Workspace {

	WorkspaceSession startSession() throws WorkspaceSessionException;

	void commitSession(WorkspaceSession workspaceSession) throws WorkspaceSessionException;

	void endSession(WorkspaceSession workspaceSession) throws WorkspaceSessionException;

}
