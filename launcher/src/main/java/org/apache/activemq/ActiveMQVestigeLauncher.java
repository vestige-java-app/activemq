package org.apache.activemq;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

import fr.gaellalire.vestige.spi.job.DummyJobHelper;
import fr.gaellalire.vestige.spi.resolver.ResolverException;
import fr.gaellalire.vestige.spi.resolver.maven.ResolvedMavenArtifact;
import fr.gaellalire.vestige.spi.resolver.maven.VestigeMavenResolver;

/**
 * @author gaellalire
 */
public class ActiveMQVestigeLauncher implements Callable<Void> {

    private static final Logger LOGGER = Logger.getLogger(ActiveMQVestigeLauncher.class.getName());

	private File config;

    public void setMavenResolver(final VestigeMavenResolver mavenResolver) {
		ResolvedMavenArtifact resolvedMavenArtifact;
		try {
			resolvedMavenArtifact = mavenResolver.createMavenContextBuilder().build()
					.resolve("org.eclipse.jetty", "apache-jstl", "9.4.28.v20200408").execute(DummyJobHelper.INSTANCE);
			StringBuilder classPath = new StringBuilder(System.getProperty("java.class.path"));
			for (ResolvedMavenArtifact dep : Collections.list(resolvedMavenArtifact.getDependencies())) {
				classPath.append(File.pathSeparator);
				classPath.append(dep.getVestigeJar().getFile());
			}
			System.setProperty("java.class.path", classPath.toString());
		} catch (ResolverException e) {
			e.printStackTrace();
		}
    }

	public ActiveMQVestigeLauncher(final File config, final File data, File cache) {
		this.config = config;
		
		System.setProperty("activemq.conf", config.getAbsolutePath());
		System.setProperty("activemq.data", data.getAbsolutePath());
		System.setProperty("activemq.home", data.getAbsolutePath());
	}

	public Void call() throws Exception {
		
		BrokerService broker = BrokerFactory
				.createBroker("xbean:" + new File(config, "activemq.xml").toURI().toURL().toString());
		broker.start();
		try {
			synchronized (this) {
				wait();
			}
		} catch (InterruptedException e) {
			broker.stop();
			Thread currentThread = Thread.currentThread();
			ThreadGroup threadGroup = currentThread.getThreadGroup();
			int activeCount = threadGroup.activeCount();
			while (activeCount != 1) {
				Thread[] list = new Thread[activeCount];
				int enumerate = threadGroup.enumerate(list);
				for (int i = 0; i < enumerate; i++) {
					Thread t = list[i];
					if (t == currentThread) {
						continue;
					}
					t.interrupt();
				}
				for (int i = 0; i < enumerate; i++) {
					Thread t = list[i];
					if (t == currentThread) {
						continue;
					}
					try {
						t.join();
					} catch (InterruptedException e1) {
						LOGGER.log(Level.FINE, "Interrupted", e1);
						break;
					}
				}
				activeCount = threadGroup.activeCount();
			}
		}
		return null;
	}

	/*
	 * Start - stop test
	 */
	public static void main(String[] args) throws InterruptedException {
		final File data = new File("home/data");
		System.setProperty("activemq.data", data.getAbsolutePath());

		Thread t = new Thread(new ThreadGroup("a"), "b") {
			@Override
			public void run() {
				File conf = new File("home/conf");
				try {
					new ActiveMQVestigeLauncher(conf, data, data).call();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
		Thread.sleep(50000);
		t.interrupt();
		t.join();
	}

}
