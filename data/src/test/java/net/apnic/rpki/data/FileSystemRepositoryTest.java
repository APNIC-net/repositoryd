package net.apnic.rpki.data;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class FileSystemRepositoryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemRepositoryTest.class);

    @SuppressWarnings("WeakerAccess")
    final Object lock = new Object();

    @Test
    public void scanTestResources() throws Exception {
        // Note: this assumes the test runs with resources on file: urls.
        FileSystemRepository repo = new FileSystemRepository(Paths.get(getClass().getResource("/repository").toURI()));

        final List<Boolean> confirmed = new ArrayList<>();
        repo.setWatcher(new Repository.Watcher() {
            @Override
            public void repositoryUpdated(Repository repository) {
                confirmed.add(true);
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        });

        synchronized (lock) {
            lock.wait(15000); // this timeout can be fragile; 15 seconds "should" be ok
        }

        assertTrue("A repository update happened within 15 seconds", !confirmed.isEmpty());
        assertNotNull("The repository has a root node", repo.getRepositoryRoot());

        Repository.Node oldRoot = repo.getRepositoryRoot();

        confirmed.clear();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100); // it's a race condition, but this minimises the risk
                    Path repository = Paths.get(getClass().getResource("/repository").toURI());
                    Path sub = Files.createTempDirectory(repository, "new-dir");
                    sub.toFile().deleteOnExit();
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage());
                    fail("An exception was thrown");
                }
            }
        }).start();

        synchronized (lock) {
            lock.wait(15000);
        }

        assertTrue("A repository update happened within 15 seconds", !confirmed.isEmpty());
        assertNotNull("The repository has a root node", repo.getRepositoryRoot());
        assertNotSame("The new root node is different", oldRoot, repo.getRepositoryRoot());
    }
}
