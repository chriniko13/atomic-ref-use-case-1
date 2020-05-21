package com.chriniko.atomicref.usecase;

import com.sun.tools.javac.util.ServiceLoader;

import java.util.concurrent.ThreadLocalRandom;

public class POC {

	public static void main(String[] args) throws Exception {

		Workspace workspace = ServiceLoader.load(Workspace.class).iterator().next();
		WorkspaceSession workspaceSession = workspace.startSession();
		try {

			try {
				Thread.sleep(100 * (ThreadLocalRandom.current().nextInt(10) + 1));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			workspace.commitSession(workspaceSession);
		}finally {
			workspace.endSession(workspaceSession);
		}

	}

}
