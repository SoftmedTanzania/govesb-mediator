package tz.go.moh.him.govesb.mediator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.*;
import org.openhim.mediator.engine.MediatorConfig;
import org.openhim.mediator.engine.messages.FinishRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;
import org.openhim.mediator.engine.testing.MockLauncher;
import org.openhim.mediator.engine.testing.TestingUtils;

import static org.junit.Assert.*;

public class DefaultOrchestratorTest {
    /**
     * Represents the system actor.
     */
    static ActorSystem system;

    /**
     * Represents the configuration.
     */
    protected static MediatorConfig configuration;

    /**
     * Runs initialization before each class execution.
     */
    @BeforeClass
    public static void beforeClass() {
        try {
            configuration = loadConfig(null);
            system = ActorSystem.create();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        List<MockLauncher.ActorToLaunch> actorsToLaunch = new LinkedList<>();
//
//        actorsToLaunch.add(new MockLauncher.ActorToLaunch("http-connector", MockDestination.class));
//
//        TestingUtils.launchActors(system, configuration.getName(), actorsToLaunch);
    }

    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testMediatorHTTPRequest() throws Exception {
        new JavaTestKit(system) {{
            final ActorRef defaultOrchestrator = system.actorOf(Props.create(DefaultOrchestrator.class, configuration));

            MediatorHTTPRequest POST_Request = new MediatorHTTPRequest(
                    getRef(),
                    getRef(),
                    "unit-test",
                    "POST",
                    "http",
                    null,
                    null,
                    "/govesb",
                    "test message",
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
}
