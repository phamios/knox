package com.samsung.srin.bilkill;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Collections;

import com.samsung.srin.bilkill.util.Constant;
import com.squareup.okhttp.CipherSuite;
import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.TlsVersion;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class HttpClient {

    private static HttpClient mInstance;

    private OkHttpClient mOk;

    private Context context;

    public HttpClient() {
        mOk = new OkHttpClient();
    }

    public static HttpClient getInstance() {
        if (mInstance == null) {
            mInstance = new HttpClient();
        }
        return mInstance;
    }

    public static final MediaType JSON = MediaType
            .parse("application/json; charset=utf-8");

    public String doRequest(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        Response response = mOk.newCall(request).execute();
        return response.body().string();
    }




    public static String encrypt(byte[] btValue,String strAlgorithm) throws Exception
    {
        byte[] data = strAlgorithm.getBytes("UTF-8");
        String base64 = Base64.encodeToString(data, Base64.DEFAULT);
        return base64;
    }

    public static String EncrytCrypto(String value, String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {

        byte[] encodeValue = Base64.encode(publicKey.getBytes(), Base64.DEFAULT);
        byte[] modulusBytes = Base64.decode(encodeValue,Base64.DEFAULT);
        byte[] exponentBytes = Base64.decode(value,Base64.DEFAULT);
        BigInteger modulus = new BigInteger(1, modulusBytes);
        BigInteger publicExponent = new BigInteger(1, exponentBytes);
        RSAPublicKeySpec rsaPubKey = new RSAPublicKeySpec(modulus, publicExponent);
        KeyFactory fact = KeyFactory.getInstance("RSA");
        PublicKey pubKey = fact.generatePublic(rsaPubKey);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        byte[] plainBytes = value.getBytes("US-ASCII");
        byte[] cipherData = cipher.doFinal(plainBytes);
        String encryptedStringBase64 = Base64.encodeToString(cipherData,Base64.DEFAULT);

        return encryptedStringBase64;
    }


    public String post(Context mcontext, String url, String post) throws IOException {
        RequestBody body = RequestBody.create(JSON, post);
        String authorization = null;

        /**
         * ///////////////BEGIN SSL Certificate
         */
        // creating a KeyStore containing our trusted CAs
        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        InputStream cert =  mcontext.getResources().openRawResource(R.raw.cert);
        Certificate ca=null;
        try {
            ca = cf.generateCertificate(cert);
        } catch (CertificateException e) {
            e.printStackTrace();
        } finally {
            try {
                cert.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(keyStoreType);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        try {
            keyStore.load(null, null);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        try {
            keyStore.setCertificateEntry("ca", ca);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        // creating a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = null;
        try {
            tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            tmf.init(keyStore);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sslContext.init(null, tmf.getTrustManagers(), null);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        /**
         * /////////////// END SSL Certificate
         */

        String publicKey = Constant.PUBLICSTATIC;
        byte[] data = Base64.decode(publicKey, Base64.DEFAULT);
        String text = new String(data, "UTF-8");
        try {
            authorization = EncrytCrypto(Constant.USERNAME + ":" + Constant.PASSWORD,text );
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2)
                .cipherSuites(
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
                .build();



        Log.e("HEADER:",authorization);
        mOk.setSslSocketFactory(sslContext.getSocketFactory());

        Request request = new Request.Builder()
                .addHeader("Content-Type","application/json")
//                .addHeader("Authorization", "basic " + authorization)
                .addHeader("ClientId", Constant.CLIENTID)
                .url(url)
                .post(body)
                .build();
        Response response = mOk.newCall(request).execute();
        return response.body().string();
    }
}
