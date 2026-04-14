package com.example.chatapp.chat.AddFriendRequest;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import java.io.File;
import okhttp3.MultipartBody;
import org.json.JSONObject;

public class HttpClient {

    public static final String BASE_URL = "http://82.157.200.53:5050/api";
    private static final String TAG = "HttpClient";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();
                    Response response = chain.proceed(request);

                    if (response.body() != null) {
                        MediaType contentType = response.body().contentType();
                        String bodyString = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(bodyString);
                            if (jsonObject.has("code")) {
                                int code = jsonObject.getInt("code");
                                if (code == 901) {
                                    // Token expired
                                    com.example.chatapp.MyApplication.getInstance().handleTokenExpired();
                                }
                            }
                        } catch (Exception e) {
                            // Not a JSON object or other error, ignore
                        }
                        
                        // Re-create the response because the body was consumed
                        ResponseBody newBody = ResponseBody.create(contentType, bodyString);
                        return response.newBuilder().body(newBody).build();
                    }
                    return response;
                }
            })
            .build();
            
    public static OkHttpClient getClient() {
        return client;
    }
    private static final Gson gson = new Gson();

    // GET 请求
    public static <T> void get(String url, Type type, String token, CallbackResult<T> callback) {
        long startMs = System.currentTimeMillis();
        Log.d(TAG, "GET -> " + BASE_URL + url);
        Request.Builder builder = new Request.Builder().url(BASE_URL + url);
        if (token != null && !token.isEmpty()) {
            builder.addHeader("token", token);
        }
        Request request = builder.build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "GET 失败: " + url + " (" + (System.currentTimeMillis() - startMs) + "ms)", e);
                callback.onResult(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) {
                    String body = response.body().string();
                    Log.d(TAG, "GET <- " + url + " http=" + response.code() + " (" + (System.currentTimeMillis() - startMs) + "ms), bodyLen=" + (body != null ? body.length() : -1));
                    Result<T> result = gson.fromJson(body, type);
                    callback.onResult(result);
                } else {
                    Log.e(TAG, "GET 响应体为空: " + url + " http=" + response.code() + " (" + (System.currentTimeMillis() - startMs) + "ms)");
                    callback.onResult(null);
                }
            }
        });
    }

    // 回调接口
    public interface CallbackResult<T> {
        void onResult(Result<T> result);
    }

    // POST 请求
    public static <T> void post(String url, Map<String, Object> params, String token, Type type, CallbackResult<T> callback) {
        long startMs = System.currentTimeMillis();
        Log.d(TAG, "POST -> " + BASE_URL + url);
        okhttp3.FormBody.Builder formBuilder = new okhttp3.FormBody.Builder();
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                formBuilder.add(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        RequestBody body = formBuilder.build();

        Request.Builder builder = new Request.Builder().url(BASE_URL + url).post(body);
        if (token != null && !token.isEmpty()) {
            builder.addHeader("token", token);
        }
        Request request = builder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "POST 失败: " + url + " (" + (System.currentTimeMillis() - startMs) + "ms)", e);
                callback.onResult(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) {
                    String bodyStr = response.body().string();
                    Log.d(TAG, "POST <- " + url + " http=" + response.code() + " (" + (System.currentTimeMillis() - startMs) + "ms), bodyLen=" + (bodyStr != null ? bodyStr.length() : -1));
                    Result<T> result = gson.fromJson(bodyStr, type);
                    callback.onResult(result);
                } else {
                    Log.e(TAG, "POST 响应体为空: " + url + " http=" + response.code() + " (" + (System.currentTimeMillis() - startMs) + "ms)");
                    callback.onResult(null);
                }
            }
        });
    }

    // POST 请求带多文件上传 (Multipart)
    public static <T> void uploadFiles(String url, Map<String, Object> params, Map<String, File> files, String token, Type type, CallbackResult<T> callback) {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (entry.getValue() != null) {
                    multipartBuilder.addFormDataPart(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        }
        
        if (files != null) {
            for (Map.Entry<String, File> entry : files.entrySet()) {
                File file = entry.getValue();
                if (file != null && file.exists()) {
                    String mimeType = "application/octet-stream";
                    if (file.getName().toLowerCase().endsWith(".jpg") || file.getName().toLowerCase().endsWith(".png")) {
                        mimeType = "image/*";
                    } else if (file.getName().toLowerCase().endsWith(".mp4")) {
                        mimeType = "video/*";
                    }
                    MediaType mediaType = MediaType.parse(mimeType);
                    multipartBuilder.addFormDataPart(entry.getKey(), file.getName(), RequestBody.create(mediaType, file));
                }
            }
        }

        RequestBody body = multipartBuilder.build();

        Request.Builder builder = new Request.Builder().url(BASE_URL + url).post(body);
        if (token != null && !token.isEmpty()) {
            builder.addHeader("token", token);
        }
        Request request = builder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "UPLOAD FILES 失败: " + url, e);
                callback.onResult(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) {
                    String bodyStr = response.body().string();
                    Result<T> result = gson.fromJson(bodyStr, type);
                    callback.onResult(result);
                } else {
                    Log.e(TAG, "UPLOAD FILES 响应体为空: " + url);
                    callback.onResult(null);
                }
            }
        });
    }
}
