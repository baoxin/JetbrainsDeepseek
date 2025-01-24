package com.example.myplugin.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DeepSeekClient {
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private final String apiKey;
    private final OkHttpClient client;
    private final Gson gson;
    private final List<JsonObject> messageHistory;

    public DeepSeekClient(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.messageHistory = new ArrayList<>();
    }

    public String sendChatMessage(String message) throws IOException {
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", message);
        messageHistory.add(userMessage);

        JsonArray messages = new JsonArray();
        // 只保留最近的10条消息
        int startIndex = Math.max(0, messageHistory.size() - 10);
        for (int i = startIndex; i < messageHistory.size(); i++) {
            messages.add(messageHistory.get(i));
        }

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("model", "deepseek-chat");
        jsonRequest.add("messages", messages);
        jsonRequest.addProperty("temperature", 0.7);

        String response = sendRequest(jsonRequest);
        
        // 保存助手的回复到历史记录
        JsonObject assistantMessage = new JsonObject();
        assistantMessage.addProperty("role", "assistant");
        assistantMessage.addProperty("content", response);
        messageHistory.add(assistantMessage);

        return response;
    }

    public String sendMessage(String message) throws IOException {
        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("role", "user");
        messageObj.addProperty("content", message);
        
        JsonArray messages = new JsonArray();
        messages.add(messageObj);
        
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("model", "deepseek-chat");
        jsonRequest.add("messages", messages);
        jsonRequest.addProperty("temperature", 0.3);
        jsonRequest.addProperty("max_tokens", 100);

        return sendRequest(jsonRequest);
    }

    private String sendRequest(JsonObject jsonRequest) throws IOException {
        RequestBody body = RequestBody.create(
                gson.toJson(jsonRequest),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API 调用失败: " + response.code());
            }
            return parseResponse(response.body().string());
        }
    }

    @NotNull
    private String parseResponse(String responseBody) {
        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
        return jsonResponse.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }

    public void clearHistory() {
        messageHistory.clear();
    }
} 