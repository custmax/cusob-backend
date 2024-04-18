package com.cusob.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import java.io.StringWriter;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class DkimGeneratorUtil {

    public static final String PRIVATE_KEY = "privateKey";
    public static final String PUBLIC_KEY = "publicKey";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void DKIMKeyPairGenerator () throws Exception{
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048); // 使用 2048 位密钥长度

        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();


        // 将私钥导出为 PKCS#8 编码的私钥字符串
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded());
//        privateKeySpec.toString()

        // 将私钥转换为 PEM 格式
//        StringWriter privateKeyPEM = new StringWriter();
//        try (JcaPEMWriter pemWriter = new JcaPEMWriter(privateKeyPEM)) {
//            pemWriter.writeObject(privateKeySpec);
//        }

        // 输出私钥（PEM格式）
//        System.out.println("Private Key (PEM):");
//        System.out.println(privateKeyPEM.toString());

        // 公钥通常会被转换为 DNS TXT 记录或其他格式，这里简单输出其编码
//        System.out.println("Public Key:");
//        System.out.println(java.util.Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
    }

    public static Map<String, String> generator(){
        try {
            // 初始化密钥对生成器，并设置密钥大小为2048位（或根据需要调整）
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048, new SecureRandom());

            // 生成密钥对
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            // 将公钥和私钥转换为Base64编码的字符串，以便存储或传输
            String encodedPublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            String encodedPrivateKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());

            // 打印或输出密钥
//            System.out.println("Public Key (Base64):");
//            System.out.println(encodedPublicKey);
            System.out.println("Private Key (Base64):");
            System.out.println(encodedPrivateKey);

            // 如果需要将私钥转换为PKCS#8格式（例如，某些邮件服务器需要这种格式）
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
            String encodedPrivateKeyPkcs8 = Base64.getEncoder().encodeToString(pkcs8KeySpec.getEncoded());
//            System.out.println("Private Key (PKCS#8 Base64):");
//            System.out.println(encodedPrivateKeyPkcs8);

            // 如果需要将公钥转换为X.509格式（这也是常见的需求）
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
            String encodedPublicKeyX509 = Base64.getEncoder().encodeToString(x509KeySpec.getEncoded());
//            System.out.println("Public Key (X.509 Base64):");
//            System.out.println(encodedPublicKeyX509);

            Map<String, String> map = new HashMap<>();
            map.put(PRIVATE_KEY, encodedPrivateKey);
            map.put(PUBLIC_KEY, encodedPublicKey);
            return map;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        generator();
    }
}
