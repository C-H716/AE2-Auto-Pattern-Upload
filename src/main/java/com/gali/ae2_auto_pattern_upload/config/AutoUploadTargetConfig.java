package com.gali.ae2_auto_pattern_upload.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

/**
 * 自动上传目标配置管理器
 * 管理哪些供应器被设置为自动上传的目标（需要用户手动设置）
 */
public class AutoUploadTargetConfig {

    private static final Set<String> targetProviders = new HashSet<>();
    private static final String CONFIG_PATH = "config/ae2_auto_pattern_upload/auto_upload_targets.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    // 静态初始化块：加载配置
    static {
        try {
            loadTargetProviders();
        } catch (Throwable t) {
            // 加载失败时静默处理
        }
    }

    /**
     * 从配置文件加载目标供应器名称列表
     */
    public static synchronized void loadTargetProviders() {
        try {
            File cfgFile = new File(CONFIG_PATH);
            if (!cfgFile.exists()) {
                return;
            }

            JsonElement element = new JsonParser().parse(new FileReader(cfgFile));
            if (element == null || !element.isJsonObject()) {
                return;
            }

            JsonObject obj = element.getAsJsonObject();
            JsonElement targetsElement = obj.get("targets");
            if (targetsElement != null && targetsElement.isJsonArray()) {
                JsonArray arr = targetsElement.getAsJsonArray();
                targetProviders.clear();
                for (JsonElement elem : arr) {
                    if (elem.isJsonPrimitive()) {
                        String name = elem.getAsString();
                        if (name != null && !name.isEmpty()) {
                            targetProviders.add(name);
                        }
                    }
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            // 加载失败时静默处理
        }
    }

    /**
     * 保存目标供应器名称列表到配置文件
     */
    public static synchronized void saveTargetProviders() {
        try {
            File cfgFile = new File(CONFIG_PATH);
            File parentDir = cfgFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            JsonObject obj = new JsonObject();
            JsonArray arr = new JsonArray();
            for (String name : targetProviders) {
                arr.add(new JsonPrimitive(name));
            }
            obj.add("targets", arr);

            FileWriter writer = new FileWriter(cfgFile);
            writer.write(GSON.toJson(obj));
            writer.close();
        } catch (IOException e) {
            // 保存失败时静默处理
        }
    }

    /**
     * 检查供应器是否被设置为自动上传目标
     */
    public static boolean isTarget(String providerName) {
        if (providerName == null || providerName.isEmpty()) {
            return false;
        }
        return targetProviders.contains(providerName);
    }

    /**
     * 设置供应器为自动上传目标
     */
    public static void setTarget(String providerName) {
        if (providerName == null || providerName.isEmpty()) {
            return;
        }
        targetProviders.add(providerName);
        saveTargetProviders();
    }

    /**
     * 取消设置供应器为自动上传目标
     */
    public static void unsetTarget(String providerName) {
        if (providerName == null || providerName.isEmpty()) {
            return;
        }
        targetProviders.remove(providerName);
        saveTargetProviders();
    }

    /**
     * 切换目标状态
     */
    public static void toggleTarget(String providerName) {
        if (isTarget(providerName)) {
            unsetTarget(providerName);
        } else {
            setTarget(providerName);
        }
    }

    /**
     * 获取所有目标供应器名称
     */
    public static Set<String> getTargetProviders() {
        return new HashSet<>(targetProviders);
    }

    /**
     * 检查是否有任何目标供应器
     */
    public static boolean hasAnyTarget() {
        return !targetProviders.isEmpty();
    }

    /**
     * 清除所有目标
     */
    public static void clearAllTargets() {
        targetProviders.clear();
        saveTargetProviders();
    }
}
