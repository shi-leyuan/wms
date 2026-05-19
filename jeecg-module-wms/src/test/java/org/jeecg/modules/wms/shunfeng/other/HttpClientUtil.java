package org.jeecg.modules.wms.shunfeng.other;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class HttpClientUtil {
    private static Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    public HttpClientUtil() {
    }

    public String post(String url, StringEntity entity) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost post = this.postForm(url, entity);
        String body = "";
        body = this.invoke(httpClient, post);

        try {
            httpClient.close();
        } catch (IOException var7) {
            logger.error("HttpClientService post error", var7);
        }

        return body;
    }

    public String post(String url, String name, String value) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        List<NameValuePair> parameters = new ArrayList();
        parameters.add(new BasicNameValuePair("content", value));
        HttpPost post = this.postForm(url, new UrlEncodedFormEntity(parameters, Charset.forName("UTF-8")));
        String body = "";
        body = this.invoke(httpClient, post);

        try {
            httpClient.close();
        } catch (IOException var9) {
            logger.error("HttpClientService post error", var9);
        }

        return body;
    }

    public String postSFAPI(String url, String xml, String verifyCode) {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        List<NameValuePair> parameters = new ArrayList();
        parameters.add(new BasicNameValuePair("xml", xml));
        parameters.add(new BasicNameValuePair("verifyCode", verifyCode));
        HttpPost post = this.postForm(url, new UrlEncodedFormEntity(parameters, Charset.forName("UTF-8")));
        String body = "";
        body = this.invoke(httpClient, post);

        try {
            httpClient.close();
        } catch (IOException var9) {
            logger.error("HttpClientService post error", var9);
        }

        return body;
    }

    public String get(String url) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet get = new HttpGet(url);
        String body = "";
        body = this.invoke(httpClient, get);

        try {
            httpClient.close();
        } catch (IOException var6) {
            logger.error("HttpClientService get error", var6);
        }

        return body;
    }

    public String delete(String url) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete delete = new HttpDelete(url);
        String body = "";
        body = this.invoke(httpClient, delete);

        try {
            httpClient.close();
        } catch (IOException var6) {
            logger.error("HttpClientService get error", var6);
        }

        return body;
    }

    public String invoke(CloseableHttpClient httpclient, HttpUriRequest httpost) {
        HttpResponse response = sendRequest(httpclient, httpost);
        String body = "";
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            body = parseResponse(response);
        }

        return body;
    }

    private static String parseResponse(HttpResponse response) {
        HttpEntity entity = response.getEntity();
        String body = "";

        try {
            if (entity != null) {
                body = EntityUtils.toString(entity);
            }
        } catch (ParseException var4) {
            logger.error("HttpClientService paseResponse error", var4);
        } catch (IOException var5) {
            logger.error("HttpClientService paseResponse error", var5);
        }

        return body;
    }

    private static HttpResponse sendRequest(CloseableHttpClient httpclient, HttpUriRequest httpost) {
        CloseableHttpResponse response = null;

        try {
            response = httpclient.execute(httpost);
        } catch (ClientProtocolException var4) {
            logger.error("HttpClientService sendRequest error", var4);
        } catch (IOException var5) {
            logger.error("HttpClientService sendRequest error", var5);
        }

        return response;
    }

    public HttpPost postForm(String url, StringEntity entity) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(entity);
        return httpPost;
    }
}
