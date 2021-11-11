package tz.go.moh.him.govesb.mediator.orchestrator;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.openhim.mediator.engine.MediatorConfig;
import org.openhim.mediator.engine.messages.FinishRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;
import tz.go.govesb.helper.dtos.ResponseData;
import tz.go.govesb.helper.dtos.TokenResponse;
import tz.go.govesb.helper.service.ESBHelper;
import tz.go.govesb.helper.service.GovESBTokenService;
import tz.go.govesb.helper.utils.DataFormatEnum;

public class FetchEmployeesFromHcmisOrchestrator extends UntypedActor {
    /**
     * The mediator configuration.
     */
    private final MediatorConfig config;
    /**
     * Represents a mediator request.
     */
    protected MediatorHTTPRequest originalRequest;
    /**
     * The logger instance.
     */
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    /**
     * Initializes a new instance of the {@link FetchEmployeesFromHcmisOrchestrator} class.
     *
     * @param config The mediator configuration.
     */
    public FetchEmployeesFromHcmisOrchestrator(MediatorConfig config) {
        this.config = config;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof MediatorHTTPRequest) {
            originalRequest = (MediatorHTTPRequest) msg;
            log.info("Received request: " + originalRequest.getHost() + " " + originalRequest.getMethod() + " " + originalRequest.getPath() + " " + originalRequest.getBody());

            String clientId;
            String secret;
            String userId;
            String govEsbURI;
            String apiCode;
            String systemPrivateKey;
            String govesbPublicKey;
            String tokenUri;

            if (config.getDynamicConfig().isEmpty()) {
                log.debug("Dynamic config is empty, using config from mediator.properties");

                clientId = config.getProperty("govesb.client.id");
                secret = config.getProperty("govesb.client-secret");
                userId = config.getProperty("govesb.user.id");


                tokenUri = config.getProperty("govesb.client.accessTokenUri");
                govEsbURI = config.getProperty("govesb.uri");
                apiCode = config.getProperty("govesb.apiCode");
                systemPrivateKey = config.getProperty("system.private-key");
                govesbPublicKey = config.getProperty("system.public-key");
            } else {
                log.debug("Using dynamic config");

                JSONObject govesbProperties = new JSONObject(config.getDynamicConfig()).getJSONObject("govesbProperties");

                userId = govesbProperties.getString("userId");
                clientId = govesbProperties.getString("clientId");
                secret = govesbProperties.getString("clientSecret");

                tokenUri = govesbProperties.getString("accessTokenUri");
                govEsbURI = govesbProperties.getString("govEsbUri");
                apiCode = govesbProperties.getString("govEsbApiCode");
                systemPrivateKey = govesbProperties.getString("privateKey");
                govesbPublicKey = govesbProperties.getString("publicKey");

            }

            ObjectMapper mapper = new ObjectMapper();

            //Requesting token from GovESB
            TokenResponse tokenResponse = GovESBTokenService.getEsbAccessToken(clientId, secret, tokenUri);

            log.info("TOKEN RETRIEVED \n" + mapper.writeValueAsString(tokenResponse.toString()));

            String response = ESBHelper.esbRequest(apiCode, userId, tokenResponse.getAccess_token(), originalRequest.getBody(), DataFormatEnum.json, systemPrivateKey, govEsbURI);

            ResponseData responseData = ESBHelper.verifyAndExtractData(response, DataFormatEnum.json, govesbPublicKey);

            FinishRequest finishRequest;
            if (responseData == null || !responseData.isHasData()) {
                finishRequest = new FinishRequest(response, "application/json", HttpStatus.SC_UNAUTHORIZED);
            } else {
                JSONObject responseObject = new JSONObject(responseData.getVerifiedData());
                boolean successStatus = responseObject.getJSONObject("data").getBoolean("success");

                if (StringUtils.isBlank(response) || !successStatus) {
                    finishRequest = new FinishRequest(response, "application/json", HttpStatus.SC_BAD_REQUEST);
                } else {
                    finishRequest = new FinishRequest(response, "application/json", HttpStatus.SC_OK);
                }
            }
            ((MediatorHTTPRequest) msg).getRequestHandler().tell(finishRequest, getSelf());
        } else {
            unhandled(msg);
        }
    }
}
