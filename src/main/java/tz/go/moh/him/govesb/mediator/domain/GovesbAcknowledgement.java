package tz.go.moh.him.govesb.mediator.domain;

public class GovesbAcknowledgement {
    private String signature;
    private Data data;

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private EsbAcknowledgement esbAcknowledgement;

        public void setEsbBody(EsbAcknowledgement esbAcknowledgement) {
            this.esbAcknowledgement = esbAcknowledgement;
        }
    }

    public static class EsbAcknowledgement {
        private boolean success;
        private int statusCode;
        private String message;

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

}
