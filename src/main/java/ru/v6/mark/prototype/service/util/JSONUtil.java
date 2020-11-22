package ru.v6.mark.prototype.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class JSONUtil {

    private static final Logger logger = LoggerFactory.getLogger(JSONUtil.class);

    @SuppressWarnings("unchecked")
    public static <T> List<T> asList(JSONArray array) {
        List<T> result = new ArrayList<T>();
        if (array != null) {
            for (int i=0;i<array.length();i++){
                result.add((T) array.get(i));
            }
        }
        return result;
    }

    public static JSONObject getJSONObject(Map jsonParams) {

        JSONObject jsonObject = null;

        if (jsonParams != null && jsonParams.size() > 0) {
            jsonObject = new JSONObject(jsonParams);
        }
        return jsonObject;
    }

    public static JSONArray getJSONArray(List jsonParams) {

        JSONArray jsonArray = null;

        if (jsonParams != null && jsonParams.size() > 0) {
            jsonArray = new JSONArray(jsonParams);
        }
        return jsonArray;
    }

    public static String getValue(String key, String json) {
        return getValue(key, new JSONObject(json));
    }

    public static String getValue(String key, JSONObject json) {
        try {
            return !json.has(key) ? null : (String) json.get(key);
        } catch (JSONException e) {
            logger.error("json: " + json);
            logger.error("JSONUtil Error getValue: ",  e);
        }
        return null;
    }

    public static JSONArray getArray(String key, String json) {
        return getArray(key, new JSONObject(json));
    }

    public static JSONArray getArray(String key, JSONObject json) {
        try {
            return !json.has(key) ? null : json.getJSONArray(key);
        } catch (JSONException e) {
            System.err.println("json: " + json);
            logger.error("JSONUtil Error getArray: ",  e);
        }
        return null;
    }
    public static List<Object> getObject(String key, String json) {
        ObjectMapper mapper = new ObjectMapper();
        List<Object> list = null;
        try {
            list = Arrays.asList(
                    mapper.convertValue(
                            mapper.readTree(json).get(key),
                            Object[].class
                    )
            );

        } catch (JsonProcessingException e) {
            System.err.println("json: " + json);
            logger.error("JSONUtil Error getObject: ",  e);
        }
        return list;
    }

    public static JSONObject getJSONObject(String key, String json) {
        if (json == null || json.equals("{}")) {
            return null;
        }
        try {
            return getJSONObject(key, new JSONObject(json));
        } catch (JSONException e) {
            System.err.println("json: " + json);
            logger.error("JSONUtil Error getJSONObject: ",  e);
        }
        return null;
    }

    public static JSONObject getJSONObject(String key, JSONObject json) {
        try {
            return !json.has(key) ? null : json.getJSONObject(key);
        } catch (JSONException e) {
            System.err.println("json: " + json);
            logger.error("JSONUtil Error getJSONObject: ",  e);
        }
        return null;
    }

    public static HashMap<String, Object> getMap(String json) {
        if (json == null || json.equals("{}")) {
            return null;
        }
        try {
            return new ObjectMapper().readValue(json, HashMap.class);
        } catch (JsonProcessingException e) {
            System.err.println("json: " + json);
            logger.error("JSONUtil Error getMap: ",  e);
        }
        return null;

    }
}
