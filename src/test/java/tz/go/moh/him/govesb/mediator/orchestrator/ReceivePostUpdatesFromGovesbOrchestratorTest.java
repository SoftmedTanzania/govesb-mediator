package tz.go.moh.him.govesb.mediator.orchestrator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openhim.mediator.engine.MediatorConfig;
import org.openhim.mediator.engine.messages.FinishRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;
import org.openhim.mediator.engine.testing.MockLauncher;
import org.openhim.mediator.engine.testing.TestingUtils;
import tz.go.moh.him.govesb.mediator.mock.MockDestination;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

public class ReceivePostUpdatesFromGovesbOrchestratorTest {

    /**
     * Represents the configuration.
     */
    protected static MediatorConfig configuration;
    /**
     * Represents the system actor.
     */
    static ActorSystem system;

    /**
     * Runs initialization before each class execution.
     */
    @BeforeClass
    public static void beforeClass() {
        try {
            configuration = loadConfig(null);
            system = ActorSystem.create();

            List<MockLauncher.ActorToLaunch> actorsToLaunch = new LinkedList<>();
            actorsToLaunch.add(new MockLauncher.ActorToLaunch("http-connector", MockDestination.class));
            TestingUtils.launchActors(system, configuration.getName(), actorsToLaunch);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    /**
     * Loads the mediator configuration.
     *
     * @param configPath The configuration path.
     * @return Returns the mediator configuration.
     */
    public static MediatorConfig loadConfig(String configPath) {
        MediatorConfig config = new MediatorConfig();


        try {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        config.setName(config.getProperty("mediator.name"));
        config.setServerHost(config.getProperty("mediator.host"));
        config.setServerPort(Integer.parseInt(config.getProperty("mediator.port")));
        config.setRootTimeout(Integer.parseInt(config.getProperty("mediator.timeout")));

        config.setCoreHost(config.getProperty("core.host"));
        config.setCoreAPIUsername(config.getProperty("core.api.user"));
        config.setCoreAPIPassword(config.getProperty("core.api.password"));

        config.setCoreAPIPort(Integer.parseInt(config.getProperty("core.api.port")));
        config.setHeartbeatsEnabled(true);

        return config;
    }

    /**
     * Adds dynamic configs to the mediator.
     *
     * @param mediatorConfig The mediator config.
     */
    public static void addDynamicConfigs(MediatorConfig mediatorConfig) {
        mediatorConfig.getDynamicConfig().put("govesbProperties", new JSONObject("{\n" +
                "    \"userId\": \"userId\",\n" +
                "    \"clientId\": \"clientId\",\n" +
                "    \"clientSecret\": \"secret\",\n" +
                "    \"accessTokenUri\":\"tokenUri\",\n" +
                "    \"govEsbUri\":\"uri\",\n" +
                "    \"govEsbApiCode\":\"code\",\n" +
                "    \"privateKey\":\"key\",\n" +
                "    \"publicKey\":\"key\"\n" +
                "  }"));

        mediatorConfig.getDynamicConfig().put("destinationConnectionProperties", new JSONObject("{\n" +
                "    \"destinationHost\": \"127.0.0.1\",\n" +
                "    \"destinationPort\": 5000,\n" +
                "    \"destinationPath\": \"/destinationPath\",\n" +
                "    \"destinationScheme\":\"http\",\n" +
                "    \"destinationUsername\":\"destinationUsername\",\n" +
                "    \"destinationPassword\":\"destinationPassword\"\n" +
                "  }"));
    }

    @Test
    public void testMediatorHTTPRequest() throws Exception {
        Assert.assertNotNull(system);
        new JavaTestKit(system) {{
            final ActorRef defaultOrchestrator = system.actorOf(Props.create(ReceivePostUpdatesFromGovesbOrchestrator.class, configuration));
            InputStream stream = ReceivePostUpdatesFromGovesbOrchestratorTest.class.getClassLoader().getResourceAsStream("employees_updates.json");

            MediatorHTTPRequest POST_Request = new MediatorHTTPRequest(
                    getRef(),
                    getRef(),
                    "unit-test",
                    "POST",
                    "http",
                    null,
                    null,
                    "/govesb",
                    IOUtils.toString(stream),
                    Collections.<String, String>singletonMap("Content-Type", "text/plain"),
                    Collections.<Pair<String, String>>emptyList()
            );

            defaultOrchestrator.tell(POST_Request, getRef());

            final Object[] out =
                    new ReceiveWhile<Object>(Object.class, duration("2 second")) {
                        @Override
                        protected Object match(Object msg) throws Exception {
                            if (msg instanceof FinishRequest) {
                                return msg;
                            }
                            throw noMatch();
                        }
                    }.get();

            boolean foundResponse = false;

            for (Object o : out) {
                if (o instanceof FinishRequest) {
                    foundResponse = true;
                }
            }

            assertTrue("Must send FinishRequest", foundResponse);
        }};
    }

    @Test
    public void testMediatorHTTPRequestWithDynamicConfig() throws Exception {
        Assert.assertNotNull(system);

        MediatorConfig config = loadConfig(null);
        addDynamicConfigs(config);
        new JavaTestKit(system) {{
            final ActorRef defaultOrchestrator = system.actorOf(Props.create(ReceivePostUpdatesFromGovesbOrchestrator.class, config));
            InputStream stream = ReceivePostUpdatesFromGovesbOrchestratorTest.class.getClassLoader().getResourceAsStream("employees_updates.json");

            MediatorHTTPRequest POST_Request = new MediatorHTTPRequest(
                    getRef(),
                    getRef(),
                    "unit-test",
                    "POST",
                    "http",
                    null,
                    null,
                    "/govesb",
                    IOUtils.toString(stream),
                    Collections.<String, String>singletonMap("Content-Type", "text/plain"),
                    Collections.<Pair<String, String>>emptyList()
            );
            defaultOrchestrator.tell(POST_Request, getRef());

            final Object[] out =
                    new ReceiveWhile<Object>(Object.class, duration("1 second")) {
                        @Override
                        protected Object match(Object msg) throws Exception {
                            if (msg instanceof FinishRequest) {
                                return msg;
                            }
                            throw noMatch();
                        }
                    }.get();

            boolean foundResponse = false;

            for (Object o : out) {
                if (o instanceof FinishRequest) {
                    foundResponse = true;
                }
            }
            assertTrue("Must send FinishRequest", foundResponse);
        }};
    }
}
