package com.xl.redisaux.transport.server;

import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author lulu
 * @Date 2020/7/19 13:54
 */
public class SendRequest {
    private CloseableHttpAsyncClient httpClient;
    private static final Charset DEFAULT_CHARSET = CharsetUtil.UTF_8;

    private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

    public SendRequest() {
        IOReactorConfig ioConfig = IOReactorConfig.custom().setConnectTimeout(3000).setSoTimeout(10000)
                .setIoThreadCount(Runtime.getRuntime().availableProcessors() * 2).build();
        httpClient = HttpAsyncClients.custom().setRedirectStrategy(new DefaultRedirectStrategy() {
            @Override
            protected boolean isRedirectable(final String method) {
                return false;
            }
        }).setMaxConnTotal(4000).setMaxConnPerRoute(1000).setDefaultIOReactorConfig(ioConfig).build();
        httpClient.start();
    }

    public static HttpUriRequest postRequest(String url, Map<String, String> params) {
        HttpPost httpPost = new HttpPost(url);
        if (params != null && params.size() > 0) {
            List<NameValuePair> list = new ArrayList<>(params.size());
            for (Map.Entry<String, String> entry : params.entrySet()) {
                list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(list, Consts.UTF_8));

        }
        return httpPost;
    }

    public CompletableFuture<String> executeCommand(String ip, int port, String api, Map<String, String> params, boolean useHttpPost) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if (ip.isEmpty() || api.isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("Bad URL or command name"));
            return future;
        }
        StringBuilder urlBuilder = new StringBuilder();
        String[] split = api.split("@");
        urlBuilder.append("http://");
        urlBuilder.append(ip).append(':').append(port);
        for (String s : split) {
            urlBuilder.append('/').append(s);
        }
        if (params == null) {
            params = Collections.emptyMap();
        }
        if (!useHttpPost) {
            // Using GET in older versions, append parameters after url
            if (!params.isEmpty()) {
                if (urlBuilder.indexOf("?") == -1) {
                    urlBuilder.append('?');
                } else {
                    urlBuilder.append('&');
                }
                urlBuilder.append(queryString(params));
            }
            return executeCommand(new HttpGet(urlBuilder.toString()));
        } else {
            // Using POST
            return executeCommand(
                    postRequest(urlBuilder.toString(), params));
        }
    }

    private CompletableFuture<String> executeCommand(HttpUriRequest request) {
        CompletableFuture<String> future = new CompletableFuture<>();
        httpClient.execute(request, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(final HttpResponse response) {
                int statusCode = response.getStatusLine().getStatusCode();
                try {
                    String value = getBody(response);
                    if (isSuccess(statusCode)) {
                        future.complete(value);
                    }

                } catch (Exception ex) {
                    future.completeExceptionally(ex);
                }
            }

            @Override
            public void failed(final Exception ex) {
                future.completeExceptionally(ex);
            }

            @Override
            public void cancelled() {
                future.complete(null);
            }
        });
        return future;
    }

    private String getBody(HttpResponse response) throws Exception {
        Charset charset = null;
        try {
            String contentTypeStr = response.getFirstHeader(HTTP_HEADER_CONTENT_TYPE).getValue();
            if (!contentTypeStr.isEmpty()) {
                ContentType contentType = ContentType.parse(contentTypeStr);
                charset = contentType.getCharset();
            }
        } catch (Exception ignore) {
        }
        return EntityUtils.toString(response.getEntity(), charset != null ? charset : DEFAULT_CHARSET);
    }

    private StringBuilder queryString(Map<String, String> params) {
        StringBuilder queryStringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            String name = urlEncode(entry.getKey());
            String value = urlEncode(entry.getValue());
            if (name != null && value != null) {
                if (queryStringBuilder.length() > 0) {
                    queryStringBuilder.append('&');
                }
                queryStringBuilder.append(name).append('=').append(value);
            }
        }
        return queryStringBuilder;
    }

    private String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, DEFAULT_CHARSET.name());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
}
