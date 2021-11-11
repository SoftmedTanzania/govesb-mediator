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
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.openhim.mediator.engine.MediatorConfig;
import org.openhim.mediator.engine.messages.FinishRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import tz.go.govesb.helper.dtos.TokenResponse;
import tz.go.govesb.helper.service.ESBHelper;
import tz.go.govesb.helper.service.GovESBTokenService;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@PrepareForTest({GovESBTokenService.class, ESBHelper.class})
@RunWith(PowerMockRunner.class)
public class SendDataToGovesbOrchestratorTest {

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
    }

    @Test
    public void testMediatorHTTPRequest() throws Exception {
        Assert.assertNotNull(system);
        new JavaTestKit(system) {{
            final ActorRef defaultOrchestrator = system.actorOf(Props.create(SendDataToGovesbOrchestrator.class, configuration));

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

            PowerMockito.mockStatic(ESBHelper.class);
            PowerMockito.mockStatic(GovESBTokenService.class);

            TokenResponse tokenResponse = new TokenResponse(true);
            tokenResponse.setAccess_token("access-token");

            ArgumentCaptor<String> clientIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> clientSecretCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> tokenURICaptor = ArgumentCaptor.forClass(String.class);
            PowerMockito.doReturn(tokenResponse).when(GovESBTokenService.class, "getEsbAccessToken", clientIdCaptor.capture(), clientSecretCaptor.capture(), tokenURICaptor.capture());


            ArgumentCaptor<String> apiCodeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> accessTokenCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> esbBodyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> esbURICaptor = ArgumentCaptor.forClass(String.class);

            String sampleResponse = "{\"data\":{\"success\":true,\"requestId\":\"35ec0e9532fd11ec8536a1101a84e6e6\",\"message\":\"Success\",\"esbBody\":{}},\"signature\":\"MEUCIDmv6hOjd0416X1Pz7MSlTwjNku06Z+dPM0uCExMT91GAiEAg1T6Fd+WvR+sSroR71/mpvWwc9hZS3RS1jLqUBbMkL8=\"}";
            PowerMockito.doReturn(sampleResponse).when(ESBHelper.class, "esbRequest", apiCodeCaptor.capture(), userIdCaptor.capture(), accessTokenCaptor.capture(), esbBodyCaptor.capture(), any(), keyCaptor.capture(), esbURICaptor.capture());


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

            //Verifying if the correct parameters were passed to GovESBTokenService.getEsbAccessToken()
            Assert.assertEquals("clientId", clientIdCaptor.getValue());
            Assert.assertEquals("client-secret", clientSecretCaptor.getValue());
            Assert.assertEquals("accessTokenUri", tokenURICaptor.getValue());


            //Verifying if the correct parameters were passed to ESBHelper.esbRequest()
            Assert.assertEquals("apiCode", apiCodeCaptor.getValue());
            Assert.assertEquals("id", userIdCaptor.getValue());
            Assert.assertEquals("access-token", accessTokenCaptor.getValue());
            Assert.assertEquals("test message", esbBodyCaptor.getValue());
            Assert.assertEquals("private-key", keyCaptor.getValue());
            Assert.assertEquals("uri", esbURICaptor.getValue());

            assertTrue("Must send FinishRequest", foundResponse);
        }};
    }

    @Test
    public void testMediatorHTTPRequestWithDynamicConfig() throws Exception {
        Assert.assertNotNull(system);

        MediatorConfig config = loadConfig(null);
        addDynamicConfigs(config);
        new JavaTestKit(system) {{
            final ActorRef defaultOrchestrator = system.actorOf(Props.create(SendDataToGovesbOrchestrator.class, config));

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

            //Mocking static Methods
            PowerMockito.mockStatic(ESBHelper.class);
            PowerMockito.mockStatic(GovESBTokenService.class);

            TokenResponse tokenResponse = new TokenResponse(true);
            tokenResponse.setAccess_token("access-token");


            //Stubbing the static methods and capturing the values passed
            ArgumentCaptor<String> clientIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> clientSecretCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> tokenURICaptor = ArgumentCaptor.forClass(String.class);
            PowerMockito.doReturn(tokenResponse).when(GovESBTokenService.class, "getEsbAccessToken", clientIdCaptor.capture(), clientSecretCaptor.capture(), tokenURICaptor.capture());


            ArgumentCaptor<String> apiCodeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> accessTokenCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> esbBodyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> esbURICaptor = ArgumentCaptor.forClass(String.class);

            String sampleResponse = "{\"data\":{\"success\":true,\"requestId\":\"35ec0e9532fd11ec8536a1101a84e6e6\",\"message\":\"Success\",\"esbBody\":{}},\"signature\":\"MEUCIDmv6hOjd0416X1Pz7MSlTwjNku06Z+dPM0uCExMT91GAiEAg1T6Fd+WvR+sSroR71/mpvWwc9hZS3RS1jLqUBbMkL8=\"}";
            PowerMockito.doReturn(sampleResponse).when(ESBHelper.class, "esbRequest", apiCodeCaptor.capture(), userIdCaptor.capture(), accessTokenCaptor.capture(), esbBodyCaptor.capture(), any(), keyCaptor.capture(), esbURICaptor.capture());

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


            //Verifying if the correct parameters were passed to GovESBTokenService.getEsbAccessToken()
            Assert.assertEquals("clientId", clientIdCaptor.getValue());
            Assert.assertEquals("secret", clientSecretCaptor.getValue());
            Assert.assertEquals("tokenUri", tokenURICaptor.getValue());


            //Verifying if the correct parameters were passed to ESBHelper.esbRequest()
            Assert.assertEquals("code", apiCodeCaptor.getValue());
            Assert.assertEquals("userId", userIdCaptor.getValue());
            Assert.assertEquals("access-token", accessTokenCaptor.getValue());
            Assert.assertEquals("test message", esbBodyCaptor.getValue());
            Assert.assertEquals("key", keyCaptor.getValue());
            Assert.assertEquals("uri", esbURICaptor.getValue());


            assertTrue("Must send FinishRequest", foundResponse);
        }};
    }
}
