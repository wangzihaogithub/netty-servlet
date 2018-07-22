package com.github.netty.core.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Properties;

/**
 * Created by acer01 on 2018/7/1/001.
 *
 * 主要名词 [Netty, OpenSSL, KeyTool, Diffie-Hellman]
 * 参考地址 https://blog.csdn.net/chuorena/article/details/77622235
 *
 * 根证书生成命令 openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout root.key –out root.crt –subj /C=CN/ST=ZheJiang/L=HangZhou/O=Hikvision/OU=GA/CN=GA –config openssl.cnf
 * 服务器端秘钥对生成命令 keytool -genkey -alias server -keypass **** -validity 1825 -keyalg RSA -keystore gateway.keystore -keysize 2048 -storepass **** -dname "CN=GA, OU=GA, O=Hikvision, L=HangZhou, ST=ZheJiang, C=CN"
 * 生成证书签名请求 keytool -certreq -alias server -keystore gateway.keystore -validity 1825 -file gateway.csr -storepass ****
 * 用根证书私钥进行签名 openssl x509 -req -in gateway.csr -CA root.crt -CAkey root.key -CAcreateserial -out gateway.pem -days 1825 -extensions SAN -extfile san1.cnf
 * 导入根证书 keytool -keystore gateway.keystore -importcert -alias CA -file root.crt -storepass hummer@123 -noprompt
 * 导入服务端证书 keytool -keystore gateway.keystore -importcert -alias server -file gateway.pem -storepass hummer@123
 *
 * keypass： 指定生成秘钥的密码
 * keystore：指定存储文件的密码，再次打开需要此密码
 *
 */
public class SslContextFactory {

    private static volatile SSLContext SERVER_CONTEXT = null;
    /*
     * 提供安全的加密算法
     */
    public static final String[] CIPHER_ARRAY = {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256"};

    private static final String DEFAULT_PROPERTIES = "application.properties";
    private static final String PROTOCOL = "server.ssl.protocol";
    private static final String KEYSTORE_TYPE = "server.ssl.key-store-type";
    private static final String KEYSTORE_PASSWORD = "gateway.ssl.key-store-password";
    private static final String GATEWAY_KEYSTORE = "gateway.ssl.key-store";

    private SslContextFactory() {}

    private static Properties loadProperties(){
        Properties properties = new Properties();
        return properties;
    }

    private static void init() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        Properties properties = loadProperties();
        String keystore_type = properties.getProperty(KEYSTORE_TYPE,"PKCS12");
        char[] keystore_password = properties.getProperty(KEYSTORE_PASSWORD,"Password@123").toCharArray();
        String gateway_keystore = properties.getProperty(GATEWAY_KEYSTORE,"");
        String protocol = properties.getProperty(PROTOCOL,"TLS");

        InputStream gatewayKeyStore = null;
        InputStream gatewayTrustStore = null;
        try {
            //初始化keyManagerFactory
            KeyStore ks = KeyStore.getInstance(keystore_type);
            gatewayKeyStore = new FileInputStream(gateway_keystore);
            ks.load(gatewayKeyStore, keystore_password);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keystore_password);
            //初始化TrustManagerFacotry
            KeyStore ts = KeyStore.getInstance(keystore_type);
            gatewayTrustStore = new FileInputStream(gateway_keystore);
            ts.load(gatewayTrustStore, keystore_password);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            //生成SSLContext
            SERVER_CONTEXT = SSLContext.getInstance(protocol);
            SERVER_CONTEXT.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } finally {
            if (null != gatewayKeyStore) {
                gatewayKeyStore.close();
            }
            if (null != gatewayTrustStore) {
                gatewayTrustStore.close();
            }
        }
    }

    public static SSLContext getServerContext() {
        if(SERVER_CONTEXT == null){
            synchronized (SslContextFactory.class) {
                if (SERVER_CONTEXT == null) {
                    try {
                        init();
                    } catch (CertificateException | KeyStoreException | KeyManagementException |
                            IOException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
                        throw new IllegalStateException(e.getMessage(),e);
                    }
                }
            }
        }
        return SERVER_CONTEXT;
    }
}
