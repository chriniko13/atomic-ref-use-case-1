package com.chriniko.atomicref.usecase;

import com.sun.tools.javac.util.ServiceLoader;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WorkspaceImplTest {

	/*
		Note:
			The purpose of WorkspaceImpl#checkThread method is to throw an exception (SessionException)
			when more than one threads access an instance (object) of WorkspaceImpl, so it provides a lightweight thread
			access protection.
	*/
	@Test
	public void test_checkThread_providesProtectionAgainstMultipleThreadAccess() {

		// given

		// Note: with only 2 threads (workers) we can simulate the scenario of WorkspaceImpl object common access, but we will try one more scenario.
		int[] workersSizeTestData = { 2, 30 };
		int[] expectedErrorsOccurred = { workersSizeTestData[0] - 1, workersSizeTestData[1] - 1 }; // minus one of X workers, due to a Workspace object can have only one owner

		int runs = 15;

		for (int i = 0; i < workersSizeTestData.length; i++) {
			for (int run = 0; run < runs; run++) {

				final Workspace workspace = ServiceLoader.load(Workspace.class).iterator().next();

				assertNotNull(workspace);

				AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
				LongAdder errorsCounter = new LongAdder();

				int workersSize = workersSizeTestData[i];
				int expectedErrors = expectedErrorsOccurred[i];

				Phaser phaser = new Phaser(workersSize + 1 /* plus one for junit test thread / waiter role */);

				CyclicBarrier rendezvous = new CyclicBarrier(workersSize,
						() -> System.out.println("all workers (size = " + workersSize + ") at 'fair' position to access/test checkThread method")
				);

				ExecutorService workers = Executors.newFixedThreadPool(workersSize, new ThreadFactory() {

					private AtomicInteger idx = new AtomicInteger(0);

					@Override public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setName("checkThread-" + idx.getAndIncrement());
						return t;
					}
				});

				// when
				for (int k = 0; k < workersSize; k++) {

					workers.submit(() -> {
						// ~~ rendezvous point ~~
						try {
							rendezvous.await();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						} catch (BrokenBarrierException ignored) {
							Assert.fail();
						}

						// ~~ actual work (simulate a scenario of usage) ~~
						WorkspaceSession workspaceSession = null;
						try {
							workspaceSession = workspace.startSession();

							// some random work
							long randomMillisIONoise = 100 * (ThreadLocalRandom.current().nextInt(3) + 1);
							Thread.yield();
							Thread.sleep(randomMillisIONoise);

							workspace.endSession(workspaceSession);

						} catch (Exception e) {

							if (e instanceof WorkspaceSessionException) {
								exceptionOccurred.set(true);
								errorsCounter.increment();
							}

							if (workspaceSession != null) {
								try {
									workspace.endSession(workspaceSession);
								} catch (WorkspaceSessionException ignored) {
								}
							}
						} finally {
							phaser.arriveAndDeregister();
						}
					});

				}

				// then
				phaser.arriveAndAwaitAdvance();
				assertTrue(exceptionOccurred.get());
				assertEquals(expectedErrors, errorsCounter.sum());

				// clean up
				workers.shutdown();
			}
		}
	}

}
