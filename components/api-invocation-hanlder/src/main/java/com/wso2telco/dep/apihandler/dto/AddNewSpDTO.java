package com.wso2telco.dep.apihandler.dto;

import java.util.List;

public class AddNewSpDTO {

    private String ownerId;
    private static final String TOKEN_URL = "https://localhost:8243/token";
    private static final long DEFAULT_CONNECTION_RESET_TIME = 4000;
    private static final int RETRY_ATTEMPT =3;
    private static final int RETRYMAX=10;
    private static final int RETRYDELAY=20000;
    private List<TokenDTO> spTokenList ;

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getTokenUrl() { return TOKEN_URL; }

    public long getDefaultconnectionresettime() {
        return DEFAULT_CONNECTION_RESET_TIME;
    }

    public int getRetryAttmpt() {
        return RETRY_ATTEMPT;
    }

    public int getRetrymax() {
        return RETRYMAX;
    }

    public int getRetrydelay() {
        return RETRYDELAY;
    }

    public List<TokenDTO> getSpTokenList() {
        return spTokenList;
    }

    public void setSpTokenList(List<TokenDTO> spTokenList) {
        this.spTokenList = spTokenList;
    }
}
