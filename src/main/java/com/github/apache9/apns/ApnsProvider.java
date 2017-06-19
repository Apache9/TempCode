package com.github.apache9.apns;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author Apache9
 */
public class ApnsProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static SSLContext createSSLContext(String file, char[] password)
            throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException, KeyManagementException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(file)) {
            keyStore.load(in, password);
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);

        SSLContext sslCtx = SSLContext.getInstance("TLS");
        sslCtx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslCtx;
    }

    private static String buildPayload(String alert) throws JsonProcessingException {
        ObjectNode payloadNode = MAPPER.createObjectNode();
        ObjectNode apsNode = payloadNode.putObject("aps");
        apsNode.put("alert", alert);
        apsNode.put("sound", "default");
        return MAPPER.writeValueAsString(payloadNode);
    }

    private static Request buildRequest(String token, String alert) throws IOException {
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), buildPayload(alert));
        return new Request.Builder().url("https://api.push.apple.com:443/3/device/" + token).method("POST", body)
                .addHeader("apns-expiration", "0").addHeader("apns-priority", "10")
                .addHeader("content-length", Long.toString(body.contentLength())).build();
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, UnrecoverableKeyException,
            KeyManagementException, KeyStoreException, FileNotFoundException, CertificateException, IOException {
        SSLContext sslCtx = createSSLContext(args[0], args[1].toCharArray());
        OkHttpClient client = new OkHttpClient.Builder().sslSocketFactory(sslCtx.getSocketFactory()).build();
        Request req = buildRequest(args[2], args[3]);
        Response resp = client.newCall(req).execute();
        System.out.println(resp);
        System.out.println(resp.body().string());
        resp.body().close();
        client.connectionPool().evictAll();
    }
}
