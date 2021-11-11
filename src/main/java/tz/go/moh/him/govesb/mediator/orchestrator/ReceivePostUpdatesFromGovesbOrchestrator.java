package tz.go.moh.him.govesb.mediator.orchestrator;

import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.openhim.mediator.engine.MediatorConfig;
import org.openhim.mediator.engine.messages.FinishRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPResponse;
import tz.go.govesb.helper.dtos.ResponseData;
import tz.go.govesb.helper.service.ESBHelper;
import tz.go.govesb.helper.utils.DataFormatEnum;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.http.HttpStatus.*;

public class ReceivePostUpdatesFromGovesbOrchestrator extends UntypedActor {
    /**
     * The mediator configuration.
     */
    private final MediatorConfig config;
    /**
     * GOVESB Public Key
     */
    public String govesbPublicKey;
    /**
     * The System Private Key
     */
    public String systemPrivateKey;
    /**
     * Represents a mediator request.
     */
    protected MediatorHTTPRequest originalRequest;
    /**
     * The logger instance.
     */
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    /**
     * Initializes a new instance of the {@link ReceivePostUpdatesFromGovesbOrchestrator} class.
     *
     * @param config The mediator configuration.
     */
    public ReceivePostUpdatesFromGovesbOrchestrator(MediatorConfig config) {
        this.config = config;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof MediatorHTTPRequest) {
            originalRequest = (MediatorHTTPRequest) msg;
            log.info("Received request: " + originalRequest.getHost() + " " + originalRequest.getMethod() + " " + originalRequest.getPath() + " " + originalRequest.getBody());


            Map<String, String> headers = new HashMap<>();

            headers.put(HttpHeaders.CONTENT_TYPE, "application/json");

            List<Pair<String, String>> parameters = new ArrayList<>();

            ObjectMapper mapper = new ObjectMapper();

            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            String host;
            int port;
            String path;
            String scheme;
            String username = "";
            String password = "";


            if (config.getDynamicConfig().isEmpty()) {
                log.debug("Dynamic config is empty, using config from mediator.properties");

                host = config.getProperty("destination.host");
                port = Integer.parseInt(config.getProperty("destination.port"));
                path = config.getProperty("destination.path");
                scheme = config.getProperty("destination.scheme");
                govesbPublicKey = config.getProperty("system.public-key");
                systemPrivateKey = config.getProperty("system.private-key");
            } else {
                log.debug("Using dynamic config");

                JSONObject destinationProperties = new JSONObject(config.getDynamicConfig()).getJSONObject("destinationConnectionProperties");

                host = destinationProperties.getString("destinationHost");
                port = destinationProperties.getInt("destinationPort");
                path = destinationProperties.getString("destinationPath");
                scheme = destinationProperties.getString("destinationScheme");

                if (destinationProperties.has("destinationUsername") && destinationProperties.has("destinationPassword")) {
                    username = destinationProperties.getString("destinationUsername");
                    password = destinationProperties.getString("destinationPassword");

                    // if we have a username and a password
                    // we want to add the username and password as the Basic Auth header in the HTTP request
                    if (username != null && !"".equals(username) && password != null && !"".equals(password)) {
                        String auth = username + ":" + password;
                        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
                        String authHeader = "Basic " + new String(encodedAuth);
                        headers.put(HttpHeaders.AUTHORIZATION, authHeader);
                    }
                }

                JSONObject govesbProperties = new JSONObject(config.getDynamicConfig()).getJSONObject("govesbProperties");
                govesbPublicKey = govesbProperties.getString("publicKey");
                systemPrivateKey = govesbProperties.getString("privateKey");
            }

            host = scheme + "://" + host + ":" + port + path;

            String requestBody = ((MediatorHTTPRequest) msg).getBody();
            ResponseData responseData = ESBHelper.verifyAndExtractData(requestBody, DataFormatEnum.json, govesbPublicKey);

            if (responseData == null || !responseData.isHasData()) {
                String response = ESBHelper.createResponse("{}", DataFormatEnum.json, systemPrivateKey, false, "Signature Verification Failed");

                FinishRequest finishRequest = new FinishRequest(response, "application/json", SC_UNAUTHORIZED);
                ((MediatorHTTPRequest) msg).getRequestHandler().tell(finishRequest, getSelf());
            } else {
                MediatorHTTPRequest request = new MediatorHTTPRequest(((MediatorHTTPRequest) msg).getRequestHandler(), getSelf(), host, "POST",
                        host, responseData.getVerifiedData(), headers, parameters);

                ActorSelection httpConnector = getContext().actorSelection(config.userPathFor("http-connector"));
                httpConnector.tell(request, getSelf());
            }

        } else if (msg instanceof MediatorHTTPResponse) {
            int statusCode;
            boolean success;
            if (((MediatorHTTPResponse) msg).getStatusCode() == SC_OK) {
                statusCode = SC_OK;
                success = true;
            } else {
                statusCode = SC_BAD_REQUEST;
                success = false;
            }

            String response = ESBHelper.createResponse(((MediatorHTTPResponse) msg).getBody(), DataFormatEnum.json, systemPrivateKey, success, "");
            FinishRequest finishRequest = new FinishRequest(response, "application/json", statusCode);
            originalRequest.getRequestHandler().tell(finishRequest, getSelf());
        } else {
            unhandled(msg);
        }
    }
}
