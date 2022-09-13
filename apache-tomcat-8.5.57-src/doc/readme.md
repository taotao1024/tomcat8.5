
# 国密SSL使用说明
本代码是基于Tomcat8.5.63进行修改

## Tomcat8.5.63 源码修改

- 为什么修改Tomcat源码

  原生Tomcat不支持国密算法套件，所以当KeyStore(存放站点证书与站点证书私钥的KeyStore)与TrustStore(存放证书链的KeyStore)存在国密证书时，Tomcat将无法解析，导致SSL服务不能启动，所以需要修改Tomcat原生代码，支持对包含国密证书的KeyStore正常解析。

- 修改代码范围

  - `org.apache.tomcat.util.net.SSLHostConfigCertificate`中增加KeyManager数组和TrustManager数组属性字段 ，并增加读写方法。代码如下

    ```java
    private KeyManager[] keyManager = null;

    private TrustManager[] trustManager = null;

    public void setKeyManager(KeyManager[] keyManager) {
        this.keyManager = keyManager;
    }

    public KeyManager[] getKeyManager() {
        return keyManager;
    }

    public void setTrustManager(TrustManager[] trustManager) {
        this.trustManager = trustManager;
    }

    public TrustManager[] getTrustManager() {
        return trustManager;
    }
    ```

  - `org.apache.tomcat.util.net.SSLHostConfig`中增加对其属性SSLHostConfigCertificate的KeyManager数组和TrustManager属性字段的设置方法，代码如下

    ```java
    public void setKeyManager(KeyManager[] keyManager) {
        registerDefaultCertificate();
      defaultCertificate.setKeyManager(keyManager);
    }

    public void setTrustManager(TrustManager[] trustManager) {
        registerDefaultCertificate();
        defaultCertificate.setTrustManager(trustManager);
    }

    ```

  - `org.apache.coyote.http11.AbstractHttp11Protocol`中增加对其属性SSLHostConfig的setKeyManager方法和setTrustManager方法的调用，代码如下

    ```java

    public void setKeyManager(KeyManager[] km) {
        registerDefaultSSLHostConfig();
        defaultSSLHostConfig.setKeyManager(km);
    }

    public void setTrustManager(TrustManager[] tm) {
      registerDefaultSSLHostConfig();
        defaultSSLHostConfig.setTrustManager(tm);
    }

    ```


  - `org.apache.tomcat.util.net.jsse.JSSESSLContext`中增加一个构造函数并设置provider

    ```java
    public JSSESSLContext(String protocol, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
         context = javax.net.ssl.SSLContext.getInstance(protocol, provider);
    }
    ```


  - `org.apache.tomcat.util.net.AbstractJsseEndpoint`中修改`createSSLContext(SSLHostConfig sslHostConfig)`方法，并使用JSSESSLContext新增的构造方法构造SSLContext并设置给SSLHostConfigCertificate，修改代码如下

    ```java
    protected void createSSLContext(SSLHostConfig sslHostConfig) throws IllegalArgumentException {
        boolean firstCertificate = true;
        for (SSLHostConfigCertificate certificate : sslHostConfig.getCertificates(true)) {
            SSLUtil sslUtil = sslImplementation.getSSLUtil(certificate);
            if (firstCertificate) {
                firstCertificate = false;
                sslHostConfig.setEnabledProtocols(sslUtil.getEnabledProtocols());
                sslHostConfig.setEnabledCiphers(sslUtil.getEnabledCiphers());
            }

            SSLContext sslContext;
            try {

                //sslContext = sslUtil.createSSLContext(negotiableProtocols);
                sslContext = new JSSESSLContext("GMVPNv1.1", KlGMJsseProvider.PROVIDER_NAME);
                sslContext.init(certificate.getKeyManager(), certificate.getTrustManager(), new SecureRandom());
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }

            certificate.setSslContext(sslContext);
        }
    }
    ```

  - `org.apache.tomcat.util.net.AbstractJsseEndpoint`中修改`createSSLEngine(String sniHostName, List<Cipher> clientRequestedCiphers)`方法，删除对算法套件和协议的设置，修改代码如下

    ```java
    protected SSLEngine createSSLEngine(String sniHostName, List<Cipher> clientRequestedCiphers,
                                            List<String> clientRequestedApplicationProtocols) {
    SSLHostConfig sslHostConfig = getSSLHostConfig(sniHostName);

    SSLHostConfigCertificate certificate = selectCertificate(sslHostConfig, clientRequestedCiphers);

        SSLContext sslContext = certificate.getSslContext();
        if (sslContext == null) {
            throw new IllegalStateException(
                sm.getString("endpoint.jsse.noSslContext", sniHostName));
    }

        SSLEngine engine = sslContext.createSSLEngine();
        engine.setUseClientMode(false);
        /* 删除对算法套件和协议的设置
            engine.setEnabledCipherSuites(sslHostConfig.getEnabledCiphers());
            engine.setEnabledProtocols(sslHostConfig.getEnabledProtocols());
            */
        SSLParameters sslParameters = engine.getSSLParameters();
        /* 删除对算法套件和协议的设置
            String honorCipherOrderStr = sslHostConfig.getHonorCipherOrder();
            if (honorCipherOrderStr != null) {
                boolean honorCipherOrder = Boolean.parseBoolean(honorCipherOrderStr);
                JreCompat.getInstance().setUseServerCipherSuitesOrder(sslParameters, honorCipherOrder);
            }
             */

        if (JreCompat.isAlpnSupported() && clientRequestedApplicationProtocols != null
            && clientRequestedApplicationProtocols.size() > 0
        && negotiableProtocols.size() > 0) {
            // Only try to negotiate if both client and server have at least
            // one protocol in common
            // Note: Tomcat does not explicitly negotiate http/1.1
            // TODO: Is this correct? Should it change?
            List<String> commonProtocols = new ArrayList<>(negotiableProtocols);
            commonProtocols.retainAll(clientRequestedApplicationProtocols);
            if (commonProtocols.size() > 0) {
                String[] commonProtocolsArray = commonProtocols.toArray(new String[0]);
                JreCompat.getInstance().setApplicationProtocols(sslParameters, commonProtocolsArray);
            }
        }
        switch (sslHostConfig.getCertificateVerification()) {
            case NONE:
                sslParameters.setNeedClientAuth(false);
                sslParameters.setWantClientAuth(false);
                break;
            case OPTIONAL:
            case OPTIONAL_NO_CA:
                sslParameters.setWantClientAuth(true);
                break;
            case REQUIRED:
                sslParameters.setNeedClientAuth(true);
                break;
        }
        // The getter (at least in OpenJDK and derivatives) returns a defensive copy
        engine.setSSLParameters(sslParameters);

        return engine;
    }
    ```
