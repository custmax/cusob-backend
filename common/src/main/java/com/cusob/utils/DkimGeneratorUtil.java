package com.cusob.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.SecureUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class DkimGeneratorUtil {

    public static void hutool() throws IOException {
        KeyPair keyPair = SecureUtil.generateKeyPair("RSA", 2048, null);
        savePemFile(keyPair.getPrivate(), "C:/Users/daybr/cusob/project/dkim/keys/hutool/private_key.pem");
        savePemFile(keyPair.getPublic(), "C:/Users/daybr/cusob/project/dkim/keys/hutool/public_key.pem");
        // 将公钥和私钥写入到DER文件
        FileUtil.writeBytes(keyPair.getPublic().getEncoded(), "C:/Users/daybr/cusob/project/dkim/keys/hutool/public_key.der");
        FileUtil.writeBytes(keyPair.getPrivate().getEncoded(), "C:/Users/daybr/cusob/project/dkim/keys/hutool/private_key.der");

    }

    private static void savePemFile(Key key, String filename)
            throws IOException {
        String encodedKey = Base64.getEncoder()
                .encodeToString(key.getEncoded());
        String keyType = (key instanceof PrivateKey) ? "PRIVATE KEY"
                : "PUBLIC KEY";

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("-----BEGIN " + keyType + "-----\n");
            writer.write(encodedKey);
            writer.write("\n-----END " + keyType + "-----");
            writer.flush();
        }
    }

    public static void main(String[] args) throws Exception {
        hutool();
    }
}
