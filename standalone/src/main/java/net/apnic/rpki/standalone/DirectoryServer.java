package net.apnic.rpki.standalone;

import net.apnic.rpki.data.FileSystemRepository;
import net.apnic.rpki.protocol.MemoryCachedModule;
import net.apnic.rpki.protocol.Module;
import net.apnic.rpki.server.RsyncServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Run a repository server delivering repositories out of directories that are watched for
 * changes on-disk.
 *
 * @author bje
 * @since 1.0
 */
public class DirectoryServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryServer.class);

    public static void main(String[] args) throws Exception {
        String configurationFile = System.getProperty("repositoryd.config");

        Properties configuration = new Properties();
        configuration.load(new FileInputStream(configurationFile));

        // Basic configuration items
        int port = Integer.parseInt(configuration.getProperty("port"));

        List<Module> modules = new ArrayList<>();

        // Get the repository filesystem location
        String repositories = configuration.getProperty("repositories");
        for (Path repository : Files.newDirectoryStream(Paths.get(repositories))) {
            if (!Files.isDirectory(repository)) continue;

            final MemoryCachedModule module = new MemoryCachedModule(repository.getFileName().toString(), "",
                    new FileSystemRepository(repository));
            modules.add(module);
            synchronized (module) {
                module.wait();
            }
            LOGGER.info("Repository {} ready to serve", repository.getFileName().toString());
        }

        RsyncServer server = new RsyncServer(port, modules.toArray(new Module[modules.size()]));
        LOGGER.info("Server starting on port {}", port);
        server.run();
    }
}

