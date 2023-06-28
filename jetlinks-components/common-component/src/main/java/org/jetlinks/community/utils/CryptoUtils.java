package org.jetlinks.community.utils;

import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class CryptoUtils {

    private static Key generateDESKey(byte[] password) throws Exception {
        DESKeySpec dks = new DESKeySpec(password);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        return keyFactory.generateSecret(dks);
    }


    @SneakyThrows
    public static byte[] encryptDES(byte[] password, byte[] ivParameter, byte[] data) {
        Key secretKey = generateDESKey(password);
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(ivParameter);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        return cipher.doFinal(data);
    }

    @SneakyThrows
    public static byte[] decryptDES(byte[] password, byte[] ivParameter, byte[] data) {
        Key secretKey = generateDESKey(password);
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(ivParameter);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        return cipher.doFinal(data);
    }

    @SneakyThrows
    public static byte[] encryptAESCBC(byte[] password, byte[] ivParameter, byte[] data) {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        Key secretKey = new SecretKeySpec(fillBit(password, cipher.getBlockSize()), "AES");
        IvParameterSpec iv = new IvParameterSpec(fillBit(ivParameter, cipher.getBlockSize()));
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        return cipher.doFinal(data);
    }

    @SneakyThrows
    public static byte[] decryptAESCBC(byte[] password, byte[] ivParameter, byte[] data) {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        Key secretKey = new SecretKeySpec(fillBit(password, cipher.getBlockSize()), "AES");
        IvParameterSpec iv = new IvParameterSpec(fillBit(ivParameter, cipher.getBlockSize()));
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        return cipher.doFinal(data);
    }

    @SneakyThrows
    public static byte[] encryptAESECB(byte[] password, byte[] data) {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        Key secretKey = new SecretKeySpec(fillBit(password, cipher.getBlockSize()), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(fillBit(data, cipher.getBlockSize()));
    }

    @SneakyThrows
    public static byte[] decryptAESECB(byte[] password, byte[] data) {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        Key secretKey = new SecretKeySpec(fillBit(password, cipher.getBlockSize()), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(data);
        if (decrypted[decrypted.length - 1] != 0x00) {
            return decrypted;
        }
        //去除补位的0x00
        for (int i = decrypted.length - 1; i >= 0; i--) {
            if (decrypted[i] != 0x00) {
                return Arrays.copyOf(decrypted, i + 1);
            }
        }
        return decrypted;
    }

    private static byte[] fillBit(byte[] data, int blockSize) {
        int len = (data.length / blockSize + (data.length % blockSize == 0 ? 0 : 1)) * 16;
        if (len == data.length) {
            return data;
        }
        return Arrays.copyOf(data, len);
    }


    @SneakyThrows
    public static KeyPair generateRSAKey() {
        KeyPairGenerator keyPairGen;
        keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(512);
        return keyPairGen.generateKeyPair();
    }

    @SneakyThrows
    public static byte[] decryptRSA(byte[] data, Key key) {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    @SneakyThrows
    public static byte[] encryptRSA(byte[] data, Key key) {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    @SneakyThrows
    public static PublicKey decodeRSAPublicKey(String base64) {
        byte[] keyBytes = Base64.decodeBase64(base64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    @SneakyThrows
    public static PrivateKey decodeRSAPrivateKey(String base64) {
        byte[] keyBytes = Base64.decodeBase64(base64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }


}
