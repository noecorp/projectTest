/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.util;

import java.security.Key;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 *
 * @author Opiks
 */
public class AESTools {

    private static final String ALGORITHM = "AES";
    private static byte[] keyValue = new byte[]{'d', '0', 'k', 'U', 'p', 'A', 'Y', 'M', '3', 'n', '7', '9', 'a', 't', '3', 'W'};

    public static void main(String[] args) {
        try {

            String insttalmentSetting = "10000112";
            String insttalmentSettingEnc = encrypt(insttalmentSetting);
            String insttalmentSettingDec = decrypt(insttalmentSettingEnc);
            System.out.println("[" + insttalmentSetting + "]");
            System.out.println("[" + insttalmentSettingEnc + "]");
            System.out.println("[" + insttalmentSettingDec + "]");
            
            
            //System.out.println(AESTools.decrypt("H6TFtedg+GIvTOs9r60vMA=="));

        } catch (Exception ex) {
            Logger.getLogger(AESTools.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static byte[] encrypt(byte[] data) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encValue = c.doFinal(data);
        return encValue;
    }

    public static byte[] decrypt(byte[] data) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decValue = c.doFinal(data);
        return decValue;
    }

    public static String encrypt(String valueToEnc) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encValue = c.doFinal(valueToEnc.getBytes());
        String encryptedValue = new BASE64Encoder().encode(encValue);
        return encryptedValue;
    }

    public static String decrypt(String encryptedValue) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decordedValue = new BASE64Decoder().decodeBuffer(encryptedValue);
        byte[] decValue = c.doFinal(decordedValue);
        String decryptedValue = new String(decValue, "UTF-8");
        return decryptedValue;
    }

    public static String encryptInputKey(String valueToEnc, String inputKey) throws Exception {
        keyValue = inputKey.getBytes();
        return encrypt(valueToEnc);
    }

    public static String decryptInputKey(String encryptedValue, String inputKey) throws Exception {
        keyValue = inputKey.getBytes();
        return decrypt(encryptedValue);
    }

    public static String encryptSafeString(String valueToEnc) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encValue = c.doFinal(valueToEnc.getBytes());
        String encryptedValue = Base64.encodeBase64URLSafeString(encValue);
        return encryptedValue;
    }

    public static String decryptSafeString(String encryptedValue) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decValue = c.doFinal(Base64.decodeBase64(encryptedValue.getBytes()));
        String decryptedValue = new String(decValue, "UTF-8");
        return decryptedValue;
    }

    public static String encryptSafeStringInputKey(String valueToEnc, String inputKey) throws Exception {
        keyValue = inputKey.getBytes();
        return encryptSafeString(valueToEnc);
    }

    public static String decryptSafeStringInputKey(String encryptedValue, String inputKey) throws Exception {
        keyValue = inputKey.getBytes();
        return decryptSafeString(encryptedValue);
    }

    private static Key generateKey() throws Exception {
        Key key = new SecretKeySpec(keyValue, ALGORITHM);
        return key;
    }
}
