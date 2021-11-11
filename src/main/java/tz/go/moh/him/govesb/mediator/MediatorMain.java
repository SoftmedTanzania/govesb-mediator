package tz.go.moh.him.govesb.mediator;

import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.openhim.mediator.engine.*;
import tz.go.moh.him.govesb.mediator.orchestrator.ReceivePostUpdatesFromGovesbOrchestrator;
import tz.go.moh.him.govesb.mediator.orchestrator.SendDataToGovesbOrchestrator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Represents the main application.
 */
public class MediatorMain {

    /**
     * Represents the mediator registration info.
     */
    private static final String MEDIATOR_REGISTRATION_INFO = "mediator-registration-info.json";

    /**
     * Builds the routing table.
     *
     * @return Returns the routing table.
     * @throws RoutingTable.RouteAlreadyMappedException if the route is already mapped
     */
    private static RoutingTable buildRoutingTable() throws RoutingTable.RouteAlreadyMappedException {
        RoutingTable routingTable = new RoutingTable();
        routingTable.addRoute("/send_to_govesb", SendDataToGovesbOrchestrator.class);
        routingTable.addRoute("/receive_data_from_govesb", ReceivePostUpdatesFromGovesbOrchestrator.class);

        return routingTable;
    }

    /**
     * Builds the startup actors configuration.
     *
     * @return Returns the startup actors configuration.
     */
    private static StartupActorsConfig buildStartupActorsConfig() {
        StartupActorsConfig startupActors = new StartupActorsConfig();
        return startupActors;
    }

    /**
     * Loads the configuration.
     *
     * @param configPath The path of the configuration.
     * @return Returns the configuration instance.
     * @throws IOException                              if an IO exception occurs
     * @throws RoutingTable.RouteAlreadyMappedException if the route is already mapped
     */
    private static MediatorConfig loadConfig(String configPath) throws IOException, RoutingTable.RouteAlreadyMappedException {
        MediatorConfig config = new MediatorConfig();

        if (configPath != null) {
            Properties props = new Properties();
            File conf = new File(configPath);
            InputStream in = FileUtils.openInputStream(conf);
            props.load(in);
            IOUtils.closeQuietly(in);

            config.setProperties(props);
        } else {
            config.setProperties("mediator.properties");
        }

        config.setName(config.getProperty("mediator.name"));
        config.setServerHost(config.getProperty("mediator.host"));
        config.setServerPort(Integer.parseInt(config.getProperty("mediator.port")));
        config.setRootTimeout(Integer.parseInt(config.getProperty("mediator.timeout")));

        config.setCoreHost(config.getProperty("core.host"));
        config.setCoreAPIUsername(config.getProperty("core.api.user"));
        config.setCoreAPIPassword(config.getProperty("core.api.password"));
        if (config.getProperty("core.api.port") != null) {
            config.setCoreAPIPort(Integer.parseInt(config.getProperty("core.api.port")));
        }

        config.setRoutingTable(buildRoutingTable());
        config.setStartupActors(buildStartupActorsConfig());

        InputStream registrationInformation = MediatorMain.class.getClassLoader().getResourceAsStream(MEDIATOR_REGISTRATION_INFO);

        if (registrationInformation == null) {
            throw new FileNotFoundException("Unable to locate " + MEDIATOR_REGISTRATION_INFO);
        }

        RegistrationConfig regConfig = new RegistrationConfig(registrationInformation);

        config.setRegistrationConfig(regConfig);

        if (config.getProperty("mediator.heartbeats") != null && "true".equalsIgnoreCase(config.getProperty("mediator.heartbeats"))) {
            config.setHeartbeatsEnabled(true);
        }

        return config;
    }

    /**
     * The main entry point of the application.
     *
     * @param args The arguments.
     * @throws Exception if an exception occurs
     */
    public static void main(String... args) throws Exception {
        //setup actor system
        final ActorSystem system = ActorSystem.create("mediator");
        //setup logger for main
        final LoggingAdapter log = Logging.getLogger(system, "main");

        //setup actors
        log.info("Initializing mediator actors...");

        String configPath = null;
        if (args.length == 2 && args[0].equals("--conf")) {
            configPath = args[1];
            log.info("Loading mediator configuration from '" + configPath + "'...");
        } else {
            log.info("No configuration specified. Using default properties...");
        }

        MediatorConfig config = loadConfig(configPath);

        config.setSSLContext(new MediatorConfig.SSLContext(true));

        final MediatorServer server = new MediatorServer(system, config);

        //setup shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("Shutting down mediator");
                server.stop();
                system.shutdown();
            }
        });

        log.info("Starting mediator server...");
        server.start();

        log.info(String.format("%s listening on %s:%s", config.getName(), config.getServerHost(), config.getServerPort()));
        Thread.currentThread().join();
    }
}
