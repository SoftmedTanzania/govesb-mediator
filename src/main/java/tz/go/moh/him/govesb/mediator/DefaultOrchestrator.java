package tz.go.moh.him.govesb.mediator;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.openhim.mediator.engine.MediatorConfig;
import org.openhim.mediator.engine.messages.FinishRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;
import tz.go.govesb.helper.dtos.TokenResponse;
import tz.go.govesb.helper.service.ESBHelper;
import tz.go.govesb.helper.service.GovESBTokenService;
import tz.go.govesb.helper.utils.DataFormatEnum;

public class DefaultOrchestrator extends UntypedActor {
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
     * Initializes a new instance of the {@link DefaultOrchestrator} class.
     *
     * @param config The mediator configuration.
     */
    public DefaultOrchestrator(MediatorConfig config) {
        this.config = config;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof MediatorHTTPRequest) {
            originalRequest = (MediatorHTTPRequest) msg;
            log.info("Received request: " + originalRequest.getHost() + " " + originalRequest.getMethod() + " " + originalRequest.getPath() + " " + originalRequest.getBody());

            String client = config.getProperty("govesb.client.id");
            String secret = config.getProperty("govesb.client-secret");
            String user = config.getProperty("govesb.user.id");

            String govEsbURI = config.getProperty("govesb.uri");
            String apiCode = config.getProperty("govesb.apiCode");
            String systemPrivateKey = config.getProperty("system.private-key");
            String tokenUri = config.getProperty("govesb.client.accessTokenUri");


            ObjectMapper mapper = new ObjectMapper();

            //Requesting token from GovESB
            TokenResponse tokenResponse = GovESBTokenService.getEsbAccessToken(client, secret, tokenUri);

            log.info("TOKEN RETRIEVED \n" + mapper.writeValueAsString(tokenResponse.toString()));

            String response = ESBHelper.esbRequest(apiCode, user, tokenResponse.getAccess_token(), originalRequest.getBody(), DataFormatEnum.json, systemPrivateKey, govEsbURI);
            FinishRequest finishRequest = new FinishRequest(response, "application/json", HttpStatus.SC_OK);
            ((MediatorHTTPRequest) msg).getRequestHandler().tell(finishRequest, getSelf());
        } else {
            unhandled(msg);
        }
    }
}
