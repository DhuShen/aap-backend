package com.dhu.utils;

import cn.hutool.http.HttpRequest;
import com.dhu.constants.BaseConstants;
import com.dhu.exception.HttpException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.CharEncoding;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.util.UriUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class HttpHelper {

    //使用连接池管理连接
    private static final PoolingHttpClientConnectionManager CONNECT_MANAGER;

    private static final RequestConfig.Builder CONFIG_BUILDER;

    @Value("${kb-qa-interface-prefix}")
    private String baseURL;

    @Value("${translation.url}")
    private String translationURL;

    @Value("${translation.appid}")
    private String translationAppId;

    @Value("${translation.key}")
    private String translationKey;

    static {
        CONNECT_MANAGER = new PoolingHttpClientConnectionManager(60, TimeUnit.SECONDS);
        CONFIG_BUILDER = RequestConfig.custom();

        CONNECT_MANAGER.setMaxTotal(1000);
        CONNECT_MANAGER.setDefaultMaxPerRoute(50);
        CONFIG_BUILDER.setSocketTimeout(1000 * 60)//设置客户端等待服务端返回数据的超时时间
                .setConnectTimeout(1000)//设置客户端发起TCP连接请求的超时时间
                .setConnectionRequestTimeout(3000);//设置客户端从连接池获取链接的超时时间
    }

    //创建 httpclient 对象
    private HttpClient getHttpClient() {
        return HttpClients.custom().setConnectionManager(CONNECT_MANAGER).disableAutomaticRetries().build();
    }

    //回收链接到连接池
    private void consume(HttpResponse response) {
        //回收链接到连接池
        if (response != null) {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (IOException e) {
                throw new HttpException(e.getMessage());
            }
        }
    }

    //发送post请求
    public String post(String url, String body) {
        HttpClient httpClient = getHttpClient();
        HttpResponse response = null;
        HttpPost httpPost = new HttpPost(baseURL + url);
        httpPost.setConfig(CONFIG_BUILDER.build());
        httpPost.setHeader("Content-type", MediaType.APPLICATION_JSON_VALUE);
        try {
            httpPost.setEntity(new StringEntity(body, "UTF-8"));
            response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return EntityUtils.toString(entity, CharEncoding.UTF_8);
            }
        } catch (IOException e) {
            throw new HttpException(e.getMessage());
        } finally {
            consume(response);
        }
        return null;
    }

    //发送GET请求
    public String get(String url, Map<String, String> params) {
        //url参数拼接
        if (params != null && !params.isEmpty()) {
            url = url + '?' + String.join("&", params.entrySet().stream().map(entry -> entry.getKey() + '=' + entry.getValue()).toArray(String[]::new));
        }
        HttpClient httpClient = getHttpClient();
        HttpResponse response = null;
        HttpGet httpGet = new HttpGet(baseURL + url);
        httpGet.setConfig(CONFIG_BUILDER.build());
        try {
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return EntityUtils.toString(entity, CharEncoding.UTF_8);
            }
        } catch (IOException e) {
            throw new HttpException(e.getMessage());
        } finally {
            consume(response);
        }
        return null;
    }

    //上传文件
    public String upload(String url, String uuid, File file, Map<String, Object> params) {
        File[] upFiles = new File[1];
        upFiles[0] = file;
        File reFile = new File(upFiles[0].getParentFile(), uuid + BaseConstants.PAPER_TYPE);
        upFiles[0].renameTo(reFile);
        upFiles[0] = reFile;
        Map<String, Object> data = new HashMap<>(params);
        data.put("files", upFiles);
        return HttpRequest.post(baseURL + url).form(data).contentType("multipart/form-data").execute().body();
    }

    //下载文件
    public void downloadFile(String url, Map<String, String> params, HttpServletResponse webResponse, String filename) {
        //url参数拼接
        if (params != null && !params.isEmpty()) {
            url = url + '?' + String.join("&", params.entrySet().stream().map(entry -> entry.getKey() + '=' + entry.getValue()).toArray(String[]::new));
        }
        HttpClient httpClient = getHttpClient();
        HttpResponse response = null;
        HttpGet httpGet = new HttpGet(baseURL + url);
        httpGet.setConfig(CONFIG_BUILDER.build());
        try {
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                //设置下载的文件名
                try (InputStream inputStream = entity.getContent(); BufferedInputStream bis = new BufferedInputStream(inputStream)) {
                    filename = UriUtils.encode(filename, CharEncoding.UTF_8);
                    webResponse.setContentType("application/octet-stream");
                    webResponse.addHeader("Content-Disposition", "attachment;fileName=" + filename);
                    webResponse.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
                    byte[] buffer = new byte[1024];
                    OutputStream os = webResponse.getOutputStream();
                    int i = bis.read(buffer);
                    while (i != -1) {
                        os.write(buffer, 0, i);
                        i = bis.read(buffer);
                    }
                } catch (Exception e) {
                    throw new HttpException("文件下载失败");
                }
            }
        } catch (IOException e) {
            throw new HttpException(e.getMessage());
        } finally {
            consume(response);
        }
    }

    //预览文件
    public void previewFile(String url, Map<String, String> params, HttpServletResponse webResponse, String filename) {
        //url参数拼接
        if (params != null && !params.isEmpty()) {
            url = url + '?' + String.join("&", params.entrySet().stream().map(entry -> entry.getKey() + '=' + entry.getValue()).toArray(String[]::new));
        }
        HttpClient httpClient = getHttpClient();
        HttpResponse response = null;
        HttpGet httpGet = new HttpGet(baseURL + url);
        httpGet.setConfig(CONFIG_BUILDER.build());
        try {
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                //设置下载的文件名
                try (InputStream inputStream = entity.getContent(); BufferedInputStream bis = new BufferedInputStream(inputStream)) {
                    filename = UriUtils.encode(filename, CharEncoding.UTF_8);
                    webResponse.setContentType("application/pdf");
                    webResponse.addHeader("Content-Disposition", "inline;fileName=" + filename);
                    webResponse.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
                    byte[] buffer = new byte[1024];
                    OutputStream os = webResponse.getOutputStream();
                    int i = bis.read(buffer);
                    while (i != -1) {
                        os.write(buffer, 0, i);
                        i = bis.read(buffer);
                    }
                } catch (Exception e) {
                    throw new HttpException("文件获取失败");
                }
            }
        } catch (IOException e) {
            throw new HttpException(e.getMessage());
        } finally {
            consume(response);
        }
    }

    //请求翻译
    public String translate(String text, String from, String to) {
        text = text.replace("\n", " ");//去掉换行符
        String salt = String.valueOf(System.currentTimeMillis());
        String value = translationAppId + text + salt + translationKey;
        String sign = DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("q", text));
        params.add(new BasicNameValuePair("from", from));
        params.add(new BasicNameValuePair("to", to));
        params.add(new BasicNameValuePair("salt", salt));
        params.add(new BasicNameValuePair("appid", translationAppId));
        params.add(new BasicNameValuePair("sign", sign));
        HttpClient httpClient = getHttpClient();
        HttpResponse response = null;
        HttpPost httpPost = new HttpPost(translationURL);
        httpPost.setConfig(CONFIG_BUILDER.build());
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, CharEncoding.UTF_8));
            response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return EntityUtils.toString(entity, CharEncoding.UTF_8);
            }
        } catch (IOException e) {
            throw new HttpException(e.getMessage());
        } finally {
            consume(response);
        }
        return null;
    }
}