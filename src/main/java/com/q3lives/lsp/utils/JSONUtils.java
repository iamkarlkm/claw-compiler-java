package com.q3lives.lsp.utils;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * JSON 工具类
 *
 * 提供便捷的 JSON 序列化和反序列化方法
 */
public class JSONUtils {

    private static final Gson DEFAULT_GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 序列化为 JSON 字符串
     */
    public static String toJson(Object obj) {
        return DEFAULT_GSON.toJson(obj);
    }

    /**
     * 格式化 JSON 字符串
     */
    public static String toPrettyJson(Object obj) {
        return PRETTY_GSON.toJson(obj);
    }

    /**
     * 从 JSON 字符串解析
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws JsonSyntaxException {
        return DEFAULT_GSON.fromJson(json, clazz);
    }

    /**
     * 从 JSON 字符串解析
     */
    public static <T> T fromJson(String json, TypeToken<T> typeToken) throws JsonSyntaxException {
        return DEFAULT_GSON.fromJson(json, typeToken.getType());
    }

    /**
     * 创建 JSON 对象
     */
    public static JsonObject createObject() {
        return new JsonObject();
    }

    /**
     * 创建 JSON 数组
     */
    public static JsonArray createArray() {
        return new JsonArray();
    }

    /**
     * 将对象添加到 JSON 对象
     */
    public static void addToObject(JsonObject obj, String key, Object value) {
        if (value == null) {
            return;
        }
        obj.add(key, toJsonTree(value));
    }

    /**
     * 将对象添加到 JSON 数组
     */
    public static void addToArray(JsonArray array, Object value) {
        if (value == null) {
            return;
        }
        array.add(toJsonTree(value));
    }

    /**
     * 将对象转换为 JsonElement
     */
    private static JsonElement toJsonTree(Object value) {
        return DEFAULT_GSON.toJsonTree(value);
    }

    /**
     * 安全的 get 方法
     */
    public static String getSafeString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }

    /**
     * 安全的 get 方法
     */
    public static int getSafeInt(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsInt();
        }
        return 0;
    }

    /**
     * 安全的 get 方法
     */
    public static boolean getSafeBoolean(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsBoolean();
        }
        return false;
    }

    /**
     * 安全的 get 方法
     */
    public static JsonArray getSafeArray(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsJsonArray();
        }
        return new JsonArray();
    }

    /**
     * 安全的 get 方法
     */
    public static JsonObject getSafeObject(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsJsonObject();
        }
        return new JsonObject();
    }

    /**
     * 安全的 get 方法
     */
    public static JsonElement getSafeElement(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key);
        }
        return null;
    }

    /**
     * 解析 JSON 数组为字符串列表
     */
    public static List<String> parseJsonArray(String json, String arrayKey) throws JsonSyntaxException {
        JsonObject obj = fromJson(json, JsonObject.class);
        JsonArray array = getSafeArray(obj, arrayKey);
        List<String> result = new ArrayList<>();

        for (JsonElement element : array) {
            result.add(element.getAsString());
        }

        return result;
    }

    /**
     * 解析 JSON 对象为键值对
     */
    public static List<String> parseJsonArray(JsonArray array) {
        List<String> result = new ArrayList<>();
        for (JsonElement element : array) {
            result.add(element.getAsString());
        }
        return result;
    }
}
