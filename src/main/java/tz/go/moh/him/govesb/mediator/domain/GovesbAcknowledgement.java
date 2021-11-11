package tz.go.moh.him.govesb.mediator.domain;

public class GovesbAcknowledgement {
    private String signature;
    private Data data;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private EsbAcknowledgement esbAcknowledgement;

        public EsbAcknowledgement getEsbBody() {
            return esbAcknowledgement;
        }

        public void setEsbBody(EsbAcknowledgement esbAcknowledgement) {
            this.esbAcknowledgement = esbAcknowledgement;
        }
    }

    public static class EsbAcknowledgement {
        private boolean success;
        private int statusCode;
        private String message;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

}
