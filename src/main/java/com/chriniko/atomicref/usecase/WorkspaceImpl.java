package com.chriniko.atomicref.usecase;

import com.sun.tools.javac.util.ServiceLoader;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class WorkspaceImpl implements Workspace {

	private static String JVM_NAME;
	static {
		JVM_NAME = ManagementFactory.getRuntimeMXBean().getName();
	}

	private final ConnectionManager connectionManager;
	private final AtomicReference<Thread> owner = new AtomicReference<Thread>(null);
	private final Set<WorkspaceSession> currentlyOpenedSessions = new LinkedHashSet<WorkspaceSession>();

	public WorkspaceImpl() {
		connectionManager = ServiceLoader.load(ConnectionManager.class).iterator().next();
	}

	public WorkspaceSession startSession() throws WorkspaceSessionException {
		checkOwner();
		WorkspaceSession workspaceSession = new WorkspaceSession(JVM_NAME);
		currentlyOpenedSessions.add(workspaceSession);
		return workspaceSession;
	}

	public void commitSession(WorkspaceSession workspaceSession) throws WorkspaceSessionException {
		checkOwner();
		connectionManager.commit(workspaceSession);
	}

	private void checkOwner() throws WorkspaceSessionException {
		Thread t = Thread.currentThread();
		if (t != owner.get()
				&& !owner.compareAndSet(null, t)) {
			throw new WorkspaceSessionException("violated single thread owner for workspace");
		}
	}

	public void endSession(WorkspaceSession workspaceSession) throws WorkspaceSessionException {
		checkOwner();
		currentlyOpenedSessions.remove(workspaceSession);
		owner.set(null);
	}
}
