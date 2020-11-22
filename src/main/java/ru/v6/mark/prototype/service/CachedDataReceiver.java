package ru.v6.mark.prototype.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.v6.mark.prototype.domain.entity.Token;
import ru.v6.mark.prototype.service.util.SleepUtil;
import ru.v6.mark.prototype.web.cache.TokenCache;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Date;


@Service
public class CachedDataReceiver {

    private Logger logger = LoggerFactory.getLogger(getClass());

    //private String GET_KEY_URL = "https://shoes.demo.crpt.tech/api/v3/auth/cert/key";
    @Value("${config.GET_KEY_URL}")
    private String GET_KEY_URL;

    //private String GET_TOK_URL = "https://shoes.demo.crpt.tech/api/v3/auth/cert/";
    @Value("${config.GET_TOK_URL}")
    private String GET_TOK_URL;

    // default alias
    @Value("${config.CER_ALIAS}")
    private String cer_alias;

    // common password
    @Value("${config.CER_PASSWORD}")
    private String cer_password;

    @Autowired
    ClientService clientService;

    public void clearCache() {
        TokenCache.clear();
    }

    public synchronized Token getTokenById(String aliasCode, boolean notCache) {
        if (aliasCode == null)
            aliasCode = cer_alias;

        Token obj = null;
        if (notCache) {
            obj = new Token(aliasCode, clientService.getToken(aliasCode, GET_KEY_URL, GET_TOK_URL, cer_password), new Date());
        } else {
            obj = TokenCache.get(aliasCode);
            if (obj == null || new Date().after(obj.getExpiryDate())) {
                String token = null;
                while (token == null) {
                    try {
                        token = clientService.getToken(aliasCode, GET_KEY_URL, GET_TOK_URL, cer_password);
                        if (token != null) {
                            obj = new Token(aliasCode, token, getExpirationTimeToken(token));
                            TokenCache.add(obj);
                        } else {
                            SleepUtil.sleep(SleepUtil.CRPT_QUERY_INTERVAL);
                        }
                    } catch (Exception e) {
                        SleepUtil.sleep(SleepUtil.CRPT_QUERY_INTERVAL);
                        //e.printStackTrace();
                    }
                }
                logger.debug("new token: " + obj.getValue() + " | ExpiryDate:" + obj.getExpiryDate());
            } else {
                logger.debug("old token | ExpiryDate:" + obj.getExpiryDate());
            }
        }

        return obj;
    }

    private Date getExpirationTimeToken(String token) {
        JsonObject obj = Json.createReader(
                new ByteArrayInputStream(Base64.getDecoder().decode(token.split("\\.")[1].
                        replace('-', '+').replace('_', '/')))).readObject();
        Long expiryDateSec = Long.valueOf(obj.getInt("exp"));
        return new Date(expiryDateSec * 1000);
    }

}
