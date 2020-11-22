package ru.v6.mark.prototype.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class BaseService {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${config.CER_ALIAS}")
    private String cer_alias;

    @Autowired
    CachedDataReceiver cachedDataReceiver;


    /**
     * Verifying Proxy Usage
     * @return
     */
    public boolean isProxy() {
        if (System.getProperty("http.proxyUser") != null && System.getProperty("http.proxyUser").length() > 0) {
            return true;
        }
        return false;
    }

    @Deprecated
    protected String getToken() {
        return getToken(cer_alias);
    }

    protected String getToken(String alias) {
        return cachedDataReceiver.getTokenById(alias == null ? cer_alias : alias, false).getValue();
    }

}