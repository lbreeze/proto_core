package ru.v6.mark.prototype.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.CryptoPro.CAdES.CAdESSignature;
import ru.CryptoPro.CAdES.exception.CAdESException;
import ru.v6.mark.prototype.domain.entity.KIZAggregation;
import ru.v6.mark.prototype.domain.entity.KIZOrder;
import ru.v6.mark.prototype.domain.entity.KIZPosition;
import ru.v6.mark.prototype.service.util.JSONUtil;
import ru.v6.mark.prototype.service.util.ResultError;
import ru.v6.mark.prototype.service.util.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Qualifier("ClientService")
public class ClientFakeService extends ClientService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    SignatureService signatureService;

    @Value("${config.USER}")
    private String USER;

    @Value("${config.CREATE_DOC_URL}")
    private String CREATE_DOC_URL;

    @Value("${config.INFO_DOC_URL}")
    private String INFO_DOC_URL;

    @Value("${config.PRODUCT_INFO_URL}")
    private String PRODUCT_INFO_URL;

    @Value("${config.CIS_LIST_URL}")
    private String CIS_LIST_URL;

    @Value("${config.CREATE_AGGREGATION_URL}")
    private String CREATE_AGGREGATION_URL;

    @Value("${config.SUZ_ORDER_URL}")
    private String SUZ_ORDER_URL;

    @Value("${config.SUZ_CODES_URL}")
    private String SUZ_CODES_URL;

    @Value("${config.SUZ_CLOSE_URL}")
    private String SUZ_CLOSE_URL;

    private RequestConfig config = null;

    Map<String, String> documentMap = new HashMap<>();

    public String checkCode(String token, String code) {
        return "";
    }

    public String checkCodes(String token, List<String> codes) {
        return "";
    }


    public String createAggregation(String aliasCode, String token, KIZAggregation aggregation, ResultError resultError) {

        return "";
    }

    public String cleanAggregation(String aliasCode, String token, KIZAggregation aggregation) {

        return "";
    }

    /**
     * @param token - токен полученный по сертификату
     * @param docId - идетификатор документа
     * @return
     */
    public String[] getDocumentStatus(String token, String docId) {
        return new String[]{"", ""};
    }

    /**
     * @param docId - идетификатор документа
     * @return json-ответ по документу
     */
    public String getDocument(String docId) {
        return "";
    }

    /**
     * @param aliasCode - наменование сертификата
     * @param token     - токен полученный по сертификату
     * @param position  - позиция заказа
     * @return
     */
    public String createDocument(String aliasCode, String token, List<String> codes, KIZPosition position) {
        return "";
    }

    public String createDocument(String aliasCode, String token, KIZOrder kizOrder, Boolean ssccOnly) {
        return "";
    }

    public String getProduct(String token, Map<String, String> params) {
        return "";
    }

    public String getCodes(String aliasCode, String token, Map<String, String> queryParams) {
        int available = Integer.parseInt(queryParams.get("available"));
        int quantity = Integer.parseInt(queryParams.get("quantity"));
        if (available < quantity) {
            quantity = available;
        }
        JSONArray codes = new JSONArray();
        for (int i =0; i < quantity; i++) {
            codes.put("01" +
                    queryParams.get("gtin") +
                    "21" +
                    RandomStringUtils.random(13, 33, 126, false, false) +
                    ((char) 29) +
                    "93" +
                    RandomStringUtils.random(4, 33, 126, false, false));
        }
        JSONObject result = new JSONObject();
        result.put("codes", codes);
        return result.toString();
    }

    public String getOrder(String aliasCode, String token, String url, Map<String, String> queryParams) {
        return "";
    }

    public String closeOrder(String aliasCode, String token, Map<String, String> queryParams) {
        return "";
    }

    /**
     * @param aliasCode         - наменование сертификата
     * @param token             - тоекн полученный по сертификату
     * @param queryParams       - парамтры запроса
     * @param user              - пользователь ?
     * @param contractDate      - дата ?
     * @param typeOrder         - тип заказа
     * @param gtins             - список значений GTIN
     * @param productionOrderId - ???
     * @return
     */
    public String createOrder(String aliasCode,
                              String token,
                              Map queryParams,
                              String user,
                              String contractDate,
                              String typeOrder,
                              Map<String, Integer> gtins,
                              String productionOrderId,
                              ResultError resultError) {
        String docId = UUID.randomUUID().toString();
        // documentMap.put(docId, )
        return docId;
    }

    public String getToken(String aliasCode, String url, String url2, String password) {
        return "";
    }

    private HttpResponse getResponseFromMP(String url, String type) {
        return getResponseFromMP(url, type, "", null);
    }

    // POST only
    private HttpResponse getResponseFromMP(String url, List<String> params, String token) {
        String paramsStr = JSONUtil.getJSONArray(params).toString();
        return getResponseFromMP(url, "POST", paramsStr, token);
    }

    private HttpResponse getResponseFromMP(String url, String type, Map<String, String> params, String token) {
        String paramsStr = "";
        if (type.equals("GET")) {
            if (params != null && params.size() > 0 && !url.contains("{")) {
                url += "?" + getHttpParamString(params);
            } else if (params != null && params.size() > 0 && url.contains("{")) {
                url = url.replace("{docId}", params.get("docId"));
            }
        } else {
            paramsStr = JSONUtil.getJSONObject(params).toString();
        }
        return getResponseFromMP(url, type, paramsStr, token);
    }

    private HttpResponse getResponseFromMP(String url, String type, String paramStr, String token) {

        CloseableHttpClient httpClient = getHttpClient();

        try {
            if (type.equals("GET")) {
                HttpGet get = new HttpGet();
                get.setConfig(config);
                if (StringUtil.hasLength(token)) {
                    get.setHeader("content-type", "application/json");
                    get.setHeader("authorization", "Bearer " + token);
                }
                try {
                    get.setURI(new URI(url));
                    logger.info("GET SENT: " + url);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                return httpClient.execute(get);
            } else {
                HttpPost post = new HttpPost(url);
                post.setConfig(config);
                if (StringUtil.hasLength(token)) {
                    post.setHeader("authorization", "Bearer " + token);
                }
                post.setHeader("content-type", "application/json");
                post.setEntity(new StringEntity(paramStr));
                logger.info("JSON SENT: " + paramStr);
                return httpClient.execute(post);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getHttpParamString(Map<String, String> params) {
        List<NameValuePair> params_ = new LinkedList<>();
        for (Map.Entry<String, String> param : params.entrySet()) {
            params_.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }
        return URLEncodedUtils.format(params_, "utf-8");
    }

    private String getResponseEntity(HttpResponse response) {
        String json = null;
        if (response != null) {
            try {
                json = EntityUtils.toString(response.getEntity());
                logger.info("JSON RECEIVED: " + json);
            } catch (IOException e) {
                logger.error("Error getResponseEntity: {}", e);
            }
        }
        return json;
    }

    private String getSignature(String signatureData, String aliasCode, boolean detached) {
        CAdESSignature signature = signatureService.getSignature(aliasCode, detached);

        byte[] src = signatureData.getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream signatureStream = new ByteArrayOutputStream();

        try {
            signature.open(signatureStream);
            signature.update(src);
            signature.close();

        } catch (CAdESException e) {
            e.printStackTrace();
        }

        try {
            signatureStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] cadesCms = signatureStream.toByteArray();

        return Base64.getEncoder().encodeToString(cadesCms);
    }

    private String getKey(String url) {
        HttpResponse response = getResponseFromMP(url, "GET");
        return getResponseEntity(response);
    }

    private CloseableHttpClient getHttpClient() {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();

        if (isProxy()) {
            credsProvider.setCredentials(
                    new AuthScope(System.getProperty("http.proxyHost"), Integer.valueOf(System.getProperty("http.proxyPort"))),
                    new UsernamePasswordCredentials(System.getProperty("http.proxyUser"), System.getProperty("http.proxyPassword")));
            HttpHost proxy = new HttpHost(System.getProperty("http.proxyHost"), Integer.valueOf(System.getProperty("http.proxyPort")));
            config = RequestConfig.custom()
                    .setProxy(proxy)
                    .build();
        } else {
            config = RequestConfig.custom()
                    .build();
        }
        return HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build();
    }

}
