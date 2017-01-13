/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author hafizsjafioedin
 */
public class HashWithSHA1 {
    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
        	int halfbyte = (data[i] >>> 4) & 0x0F;
        	int two_halfs = 0;
        	do {
	            if ((0 <= halfbyte) && (halfbyte <= 9))
	                buf.append((char) ('0' + halfbyte));
	            else
	            	buf.append((char) ('a' + (halfbyte - 10)));
	            halfbyte = data[i] & 0x0F;
        	} while(two_halfs++ < 1);
        }
        return buf.toString();
    }
    
    
    public static String hex(byte[] input) {
        char[] HEX_TABLE = new char[] {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        
        // create a StringBuffer 2x the size of the hash array
        StringBuffer sb = new StringBuffer(input.length * 2);

        // retrieve the byte array data, convert it to hex
        // and add it to the StringBuffer
        for (int i = 0; i < input.length; i++) {
            sb.append(HEX_TABLE[(input[i] >> 4) & 0xf]);
            sb.append(HEX_TABLE[input[i] & 0xf]);
        }
        return sb.toString();
    }    

    private static String SHA1(String text)
    throws NoSuchAlgorithmException, UnsupportedEncodingException  {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
       // byte[] sha1hash = new byte[40];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        byte[] sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    private static String SHA2(String text)
    throws NoSuchAlgorithmException, UnsupportedEncodingException  {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-256");
       // byte[] sha1hash = new byte[40];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        byte[] sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    
    private static String HMACSHA256(String text, String secureSecret) {
        String result = null;
        byte[] mac = null;
        try {
            byte[] b = secureSecret.getBytes(Charset.forName("UTF-8"));//new BigInteger(secureSecret, 16).toByteArray();
            SecretKey key = new SecretKeySpec(b, "HmacSHA256");
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(key);
            //String values = new String(buf.toString(), "UTF-8");
            m.update(text.getBytes("ISO-8859-1"));
            mac = m.doFinal();
            
            result = hex(mac);

        } catch (Exception e) {
            e.printStackTrace();
        }

        
        return result;        
        
    }
    
    
    private static String HMACSHA1(String text, String secureSecret) {
        String result = null;
        byte[] mac = null;
        try {
            byte[] b = secureSecret.getBytes(Charset.forName("UTF-8"));//new BigInteger(secureSecret, 16).toByteArray();
            SecretKey key = new SecretKeySpec(b, "HmacSHA1");
            Mac m = Mac.getInstance("HmacSHA1");
            m.init(key);
            //String values = new String(buf.toString(), "UTF-8");
            m.update(text.getBytes("ISO-8859-1"));
            mac = m.doFinal();
            
            result = hex(mac);

        } catch (Exception e) {
            e.printStackTrace();
        }

        
        return result;        
        
    }    
    
    public static String doHashing(String text, String method, String secureSecret) {
        try {
            if (method==null)
                return HashWithSHA1.SHA1(text);


            if (method.equalsIgnoreCase("SHA2")) 
                return HashWithSHA1.SHA2(text);
            else if (method.equalsIgnoreCase("HMACSHA256"))
                return HashWithSHA1.HMACSHA256(text, secureSecret);
            else if (method.equalsIgnoreCase("HMACSHA1"))
                return HashWithSHA1.HMACSHA1(text, secureSecret);            
            else
                return HashWithSHA1.SHA1(text);
        
        } catch (Exception e) {
            
            return null;
        }     
            
        
    }
    
    
    
}
