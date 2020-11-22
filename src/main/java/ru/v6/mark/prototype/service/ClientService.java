package ru.v6.mark.prototype.service;

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
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.CryptoPro.CAdES.CAdESSignature;
import ru.CryptoPro.CAdES.exception.CAdESException;
import ru.v6.mark.prototype.domain.constant.KIZMarkStatus;
import ru.v6.mark.prototype.domain.constant.KIZOrderType;
import ru.v6.mark.prototype.domain.constant.KIZPositionStatus;
import ru.v6.mark.prototype.domain.entity.KIZAggregation;
import ru.v6.mark.prototype.domain.entity.KIZMark;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

//@Service
public class ClientService extends BaseService {

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

    public String checkCode(String token, String code) {
        List<String> params = Collections.singletonList(code);
        return getResponseEntity(getResponseFromMP(CIS_LIST_URL, params, token));
    }

    public String checkCodes(String token, List<String> codes) {
        return getResponseEntity(getResponseFromMP(CIS_LIST_URL, codes, token));
    }


    public String createAggregation(String aliasCode, String token, KIZAggregation aggregation, ResultError resultError) {

        Map<String, Object> jsonParams = new HashMap<>();
        jsonParams.put("participantId", aggregation.getKizOrder().getDepartment().getOrganization().getInn());
        JSONArray mainArray = new JSONArray();
        JSONArray marksArray = new JSONArray();
        JSONObject mainObj = new JSONObject();
        mainObj.put("unitSerialNumber", aggregation.getSscc());
        mainObj.put("aggregationType", "AGGREGATION");
        boolean isNotCiculated = false;
        boolean isCiculated = false;
        for (KIZMark mark : aggregation.getMarks()) {
            if (mark.getStatus().equals(KIZMarkStatus.CIRCULATED)) {
                isCiculated = true;
            } else { //if (mark.getStatus().equals(KIZMarkStatus.RECEIVED) || mark.getStatus().equals(KIZMarkStatus.VALIDATED)) {
                isNotCiculated = true;
            }
            try {
                marksArray.put(mark.getMark().substring(0, 31));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (isNotCiculated && isCiculated) {
            resultError.setDescription("Марки не могут быть сагрегированы т.к. находятся в разных статусах");
            return null;
        }
        mainObj.put("sntins", marksArray);
        mainArray.put(mainObj);
        jsonParams.put("aggregationUnits", mainArray);
        String document = JSONUtil.getJSONObject(jsonParams).toString();
        logger.info("DOCUMENT: " + document);
        Map<String, String> params = new HashMap<>();
        // JSON, переведенный в Base64. ВАЖНО УДАЛИТЬ ВСЕ ОТСТУПЫ КАК СНАЧАЛА В JSON, ТАК И В BASE64
        params.put("product_document", new String(Base64.getEncoder().encode(document.trim().getBytes())));
        // тип JSON
        params.put("document_format", "MANUAL");
        // это JSON, переведенный в Base64 и подписанный открепленной подписью
        params.put("signature", getSignature(document.trim(), aliasCode, true));

        return getResponseEntity(getResponseFromMP(CREATE_AGGREGATION_URL, "POST", params, token));
    }

    public String cleanAggregation(String aliasCode, String token, KIZAggregation aggregation) {

        Map<String, Object> jsonParams = new HashMap<>();
        // jsonParams.put("participantId", aggregation.getKizOrder().getDepartment().getOrganization().getInn());
        jsonParams.put("participant_inn", aggregation.getKizOrder().getDepartment().getOrganization().getInn());
        jsonParams.put("reaggregation_type", "REMOVING");
        JSONArray mainArray = new JSONArray();
        //int count = 0;
        for (KIZPosition position : aggregation.getKizOrder().getPositions()) {
            for (KIZMark mark : position.getMarks()) {
                try {
                    if (!mark.getSscc().equals(aggregation.getSscc())) {// && (count < 1000)) {
                        JSONObject mainObj = new JSONObject();
                        mainObj.put("uit_uitu", mark.getMark().substring(0, 31));
                        mainArray.put(mainObj);
                        // count++;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        jsonParams.put("uit_uitu_list", mainArray);
        jsonParams.put("uitu", aggregation.getSscc());
        String document = JSONUtil.getJSONObject(jsonParams).toString();

        logger.info("DOCUMENT: " + document);

        Map<String, String> params = new HashMap<>();
        // JSON, переведенный в Base64. ВАЖНО УДАЛИТЬ ВСЕ ОТСТУПЫ КАК СНАЧАЛА В JSON, ТАК И В BASE64
        params.put("product_document", new String(Base64.getEncoder().encode(document.trim().getBytes())));
        // тип JSON
        params.put("document_format", "MANUAL");
        // это JSON, переведенный в Base64 и подписанный открепленной подписью
        params.put("signature", getSignature(document.trim(), aliasCode, true));

        return getResponseEntity(getResponseFromMP(CREATE_AGGREGATION_URL, "POST", params, token));
    }

    /**
     * @param token - токен полученный по сертификату
     * @param docId - идетификатор документа
     * @return
     */
    public String[] getDocumentStatus(String token, String docId) {
        Map<String, String> params = new HashMap<>();
        params.put("docId", docId);
        String response = getResponseEntity(getResponseFromMP(INFO_DOC_URL, "GET", params, token));
        String status = JSONUtil.getValue("status", response);
        String errorDesc = "";
        if (status != null && !status.equals(KIZPositionStatus.CHECKED_OK)) {
            errorDesc = JSONUtil.getValue("downloadDesc", response);
        }
        return new String[]{status, errorDesc};
    }

    /**
     * @param docId - идетификатор документа
     * @return json-ответ по документу
     */
    public String getDocument(String docId) {
        Map<String, String> params = new HashMap<>();
        params.put("docId", docId);
        return getResponseEntity(getResponseFromMP(INFO_DOC_URL, "GET", params, getToken()));
    }

    /**
     * @param aliasCode - наменование сертификата
     * @param token     - токен полученный по сертификату
     * @param position  - позиция заказа
     * @return
     */
    public String createDocument(String aliasCode, String token, List<String> codes, KIZPosition position) {
        KIZOrder kizOrder = position.getKizOrder();

        String typeStr = null;
        Map<String, Object> jsonParams = new HashMap<>();
        if (KIZOrderType.REMAINS.equals(kizOrder.getOrderType())) {
            typeStr = "LP_INTRODUCE_OST";

            jsonParams.put("trade_participant_inn", kizOrder.getDepartment().getOrganization().getInn());
            JSONArray productListArray = new JSONArray();
            JSONObject productObj = null;
            for (String code : codes) {
                productObj = new JSONObject();
                try {
                    productObj.put("ki", code.substring(0, 31));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                productListArray.put(productObj);
            }
            jsonParams.put("products_list", productListArray);
        } else if (KIZOrderType.IMPORT.equals(kizOrder.getOrderType())) {
            typeStr = "LP_GOODS_IMPORT";

            jsonParams.put("doc_type", "GOODSIMPORT");

            Map<String, String> documentDescription = new HashMap<>();
            documentDescription.put("participant_inn", kizOrder.getDepartment().getOrganization().getInn());
            documentDescription.put("declaration_date", new SimpleDateFormat("yyyy-MM-dd").format(kizOrder.getDeclarationDate()));
            documentDescription.put("declaration_number", kizOrder.getDeclarationNumber());
            documentDescription.put("customs_code", kizOrder.getCustomsCode());
            documentDescription.put("decision_code", "10");

            jsonParams.put("document_description", JSONUtil.getJSONObject(documentDescription));
            JSONArray productListArray = new JSONArray();
            for (String code : codes) {
                JSONObject productObj = new JSONObject();
                productObj.put("uit_code", code.substring(0, 31));
                productObj.put("tnved_code", position.getTnved());
                productListArray.put(productObj);
            }
            jsonParams.put("products", productListArray);
        }

        String document = JSONUtil.getJSONObject(jsonParams).toString();
        logger.info("DOCUMENT: " + document);
        Map<String, String> params = new HashMap<>();
        // JSON, переведенный в Base64. ВАЖНО УДАЛИТЬ ВСЕ ОТСТУПЫ КАК СНАЧАЛА В JSON, ТАК И В BASE64
        params.put("product_document", new String(Base64.getEncoder().encode(document.trim().getBytes())));
        // тип JSON
        params.put("document_format", "MANUAL");
        // вводим в оборот остатки
        params.put("type", typeStr);
        // это JSON, переведенный в Base64 и подписанный открепленной подписью
        params.put("signature", getSignature(document.trim(), aliasCode, true));

        return getResponseEntity(getResponseFromMP(CREATE_DOC_URL, "POST", params, token));
    }

    public String createDocument(String aliasCode, String token, KIZOrder kizOrder, Boolean ssccOnly) {
        String typeStr = null;
        Map<String, Object> jsonParams = new HashMap<>();
        if (KIZOrderType.REMAINS.equals(kizOrder.getOrderType())) {
            typeStr = "LP_INTRODUCE_OST";

            jsonParams.put("trade_participant_inn", kizOrder.getDepartment().getOrganization().getInn());
            JSONArray productListArray = new JSONArray();
            kizOrder.getPositions().parallelStream().flatMap(position -> position.getMarks().parallelStream()).forEach(kizMark -> {
                JSONObject productObj = new JSONObject();
                try {
                    productObj.put("ki", kizMark.getMark().substring(0, 31));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                productListArray.put(productObj);

            });
            jsonParams.put("products_list", productListArray);
        } else if (KIZOrderType.IMPORT.equals(kizOrder.getOrderType())) {

            if (kizOrder.getDeclarationDate().before(AcceptanceService.LAUNCH_DATE)) {
                typeStr = "LP_GOODS_IMPORT";

                jsonParams.put("doc_type", "GOODSIMPORT");

                jsonParams.put("participant_inn", kizOrder.getDepartment().getOrganization().getInn());
                jsonParams.put("declaration_date", new SimpleDateFormat("yyyy-MM-dd").format(kizOrder.getDeclarationDate()));
                jsonParams.put("declaration_number", kizOrder.getDeclarationNumber());
                jsonParams.put("customs_code", kizOrder.getCustomsCode());
                jsonParams.put("decision_code", "10");

                ConcurrentLinkedQueue<JSONObject> productList = new ConcurrentLinkedQueue<JSONObject>();

                kizOrder.getPositions().parallelStream().flatMap(position -> position.getMarks().parallelStream()).forEach(kizMark -> {
                    if (kizMark.getStatus() != null && KIZMarkStatus.VALIDATED.equals(kizMark.getStatus())) {
                        JSONObject productObj = new JSONObject();
                        productObj.put("uit_code", kizMark.getMark().substring(0, 31));
                        productObj.put("tnved_code", kizMark.getPosition().getTnved());
                        productList.add(productObj);
                    }

                });
                jsonParams.put("products", new JSONArray(productList));
            } else {
                typeStr = "LP_FTS_INTRODUCE";

                // jsonParams.put("doc_type", "LP_FTS_INTRODUCE_JSON");

                jsonParams.put("trade_participant_inn", kizOrder.getDepartment().getOrganization().getInn());
                jsonParams.put("declaration_date", new SimpleDateFormat("dd.MM.yyyy").format(kizOrder.getDeclarationDate()));
                jsonParams.put("declaration_number", kizOrder.getDeclarationNumber());
                // jsonParams.put("customs_code", kizOrder.getCustomsCode());
                // jsonParams.put("decision_code", "10");

                ConcurrentLinkedQueue<JSONObject> productList = new ConcurrentLinkedQueue<JSONObject>();
                kizOrder.getAggregations().parallelStream().forEach(kizAggregation -> {
                    if (!kizAggregation.getMarks().isEmpty() && kizAggregation.getMarks().get(0) != null) {
                        JSONObject productObj = new JSONObject();
                        if (ssccOnly == null || ssccOnly) {
                            productObj.put("cis", kizAggregation.getSscc());
                            productObj.put("packType", "LEVEL1");
                        }

                        if (ssccOnly == null || !ssccOnly) {
                            JSONArray children = new JSONArray();
                            kizAggregation.getMarks().forEach(kizMark -> {
                                if (kizMark.getStatus() != null && KIZMarkStatus.VALIDATED.equals(kizMark.getStatus())) {
                                    JSONObject child = new JSONObject();
                                    child.put("cis", kizMark.getMark().substring(0, 31));
                                    child.put("packType", "UNIT");
                                    if (ssccOnly == null) {
                                        children.put(child);
                                    } else {
                                        productList.add(child);
                                    }
                                }

                            });

                            if (ssccOnly == null) {
                                productObj.put("children", children);
                            }
                        }
                        if (ssccOnly == null || ssccOnly) {
                            productList.add(productObj);
                        }
                    }
                });


                jsonParams.put("products_list", new JSONArray(productList));
            }
        }
        
        String document = JSONUtil.getJSONObject(jsonParams).toString();
        logger.info("DOCUMENT: " + document);
        Map<String, String> params = new HashMap<>();
        // JSON, переведенный в Base64. ВАЖНО УДАЛИТЬ ВСЕ ОТСТУПЫ КАК СНАЧАЛА В JSON, ТАК И В BASE64
        params.put("product_document", new String(Base64.getEncoder().encode(document.trim().getBytes())));
        // тип JSON
        params.put("document_format", "MANUAL");
        // вводим в оборот остатки
        params.put("type", typeStr);
        // это JSON, переведенный в Base64 и подписанный открепленной подписью
        params.put("signature", getSignature(document.trim(), aliasCode, true));

        return getResponseEntity(getResponseFromMP(CREATE_DOC_URL, "POST", params, token));
    }

    public String getProduct(String token, Map<String, String> params) {
        return getResponseEntity(getResponseFromMP(PRODUCT_INFO_URL, "GET", params, token));
    }

    public String getCodes(String aliasCode, String token, Map<String, String> queryParams) {
        return getResponseEntity(getResponseFromSUZ(aliasCode, SUZ_CODES_URL, "GET", queryParams, null, token));
    }

    public String getOrder(String aliasCode, String token, String url, Map<String, String> queryParams) {
        return getResponseEntity(getResponseFromSUZ(aliasCode, url, "GET", queryParams, null, token));
    }

    public String closeOrder(String aliasCode, String token, Map<String, String> queryParams) {
        return getResponseEntity(getResponseFromSUZ(aliasCode, SUZ_CLOSE_URL, "POST", queryParams, null, token));
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

        Map<String, Object> jsonParams = new HashMap<>();
        jsonParams.put("contactPerson", user == null ? USER : user);
        jsonParams.put("releaseMethodType", typeOrder);
        //jsonParams.put("contractNumber", "contractNumber");
        //jsonParams.put("contractDate", contractDate);
        jsonParams.put("createMethodType", "SELF_MADE");
        jsonParams.put("productionOrderId", productionOrderId);
        jsonParams.put("remainsAvailable", true);
        jsonParams.put("remainsImport", false);
        JSONArray gtinArray = new JSONArray();

        for (String gtin : gtins.keySet()) {
            JSONObject catalogObj = new JSONObject();
            try {
                catalogObj.put("gtin", gtin);
                catalogObj.put("quantity", gtins.get(gtin));
                catalogObj.put("serialNumberType", "OPERATOR");
                catalogObj.put("templateId", 1);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            gtinArray.put(catalogObj);
        }

        jsonParams.put("products", gtinArray);
        String response = getResponseEntity(getResponseFromSUZ(aliasCode, SUZ_ORDER_URL, "POST", queryParams, jsonParams, token));
        String orderId = JSONUtil.getValue("orderId", response);
        if (!StringUtil.hasLength(orderId)) {
            resultError.setDescription(JSONUtil.getArray("globalErrors", response).getString(0));
            return null;
        } else {
            return orderId;
        }
    }

    public String getToken(String aliasCode, String url, String url2, String password) {
        String jsonResponse = getKey(url);
        Map<String, String> params = new HashMap<>();
        params.put("uuid", JSONUtil.getValue("uuid", jsonResponse));
        params.put("data", getSignature(JSONUtil.getValue("data", jsonResponse), aliasCode, false));
        HttpResponse response = getResponseFromMP(url2, "POST", params, null);
        String json = getResponseEntity(response);
        return JSONUtil.getValue("token", json);
    }

    private HttpResponse getResponseFromSUZ(String aliasCode, String url,
                                            String type,
                                            Map<String, String> queryParams,
                                            Map<String, Object> jsonParams,
                                            String token) {

        CloseableHttpClient httpClient = getHttpClient();
        try {
            if (type.equals("POST")) {
                HttpPost post = new HttpPost();
                post.setConfig(config);
                String entityPost = null;
                if (jsonParams != null) {
                    entityPost = JSONUtil.getJSONObject(jsonParams).toString().replace("\\", "");
                    post.setEntity(new StringEntity(entityPost));
                }
                if (StringUtil.hasLength(token)) {
                    post.setHeader("content-type", "application/json");
                    post.setHeader("clientToken", token);
                    if (jsonParams != null) {
                        post.setHeader("X-Signature", getSignature(entityPost, aliasCode, true));
                    }
                    if (queryParams != null && queryParams.size() > 0) {
                        url += "?" + getHttpParamString(queryParams);
                    }
                }
                try {
                    logger.info("POST SENT: " + url);
                    post.setURI(new URI(url));
                } catch (URISyntaxException e) {
                    logger.error("Error getResponseFromSUZ", e);
                }
                return httpClient.execute(post);
            } else {
                HttpGet get = new HttpGet(url);
                get.setConfig(config);
                get.setHeader("clientToken", token);
                if (queryParams != null && queryParams.size() > 0) {
                    url += "?" + getHttpParamString(queryParams);
                }
                try {
                    logger.info("GET SENT: " + url);
                    get.setURI(new URI(url));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                return httpClient.execute(get);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
