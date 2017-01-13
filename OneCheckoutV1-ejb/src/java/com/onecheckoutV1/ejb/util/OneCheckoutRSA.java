/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.ejb.util;

import java.io.BufferedReader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.Cipher;
import org.bouncycastle.jce.provider.JCERSAPublicKey;
import org.bouncycastle.openssl.PEMReader;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutRSA {


    public static String Encrypt(String plaintext, String publicKeyString)   {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        try {
            BASE64Decoder decoder   = new BASE64Decoder();
            BASE64Encoder encoder = new BASE64Encoder();
            String b64PublicKey    = publicKeyString;//"LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQ0KTUlJQ1hBSUJBQUtCZ1FDMGZHNUFTTC9POWh4d1BHNXFTWUQxOVNWMkhqVENuRGV4SG45d25sU3ZaM2trMWpnYg0KQ01CVXpvUXZrUCtIVlJKci8zL0JCV29LdGovS0FockRObVA0cForVW5JcGs1SDdPWmx0N25ETzdib2dQSXhhVA0KN2lVd1J0UjF3MTNvQlE5VGYySCtPa3BuQnVPR3RVZSs4SUg4WVoxQ3FqTDI0cnp5emIrTHp2andiUUlEQVFBQg0KQW9HQkFLakkxTEg1VnFiTk1ic2tTSDNOVDNTMFZOd3BVMTNMaWFseHcrd2xwVHlEbjU3N1ZteXh1bXVVMWJsRg0KK0RFdk5aTVoxUGRGZ08yVGtnUHdBK2NiTGxEL1JvM3lEQ3Z1RGNzS25XQys2Sm1Jc1prK0NScDNGMW16dGNweA0KOVVldnUxejNYd0JXUis0QkhIaGFaZUFxTnhNU3FrL1FhWjV6WjJRS21CSG1SSlZoQWtFQTJFWkU1ZVBua0s3Kw0KUlhENVQ2bDVLei92ZnJ1UTF2Q280ZG9ZdUlGVm5QblBacWYrUXU1OWIySXVpaUNkdkx1bm0ybWZTT1M0UUt2bQ0KOHZQSTJkNkxVd0pCQU5XalRsZEZzUnptN3F0S08xYkNmd0g1U1lPSC9PaHFTcnRMSzhuTlFyQS85OWFVYUhndA0KTm1DZ052VXhndWtHdkRDQ1NnT3JBeUd3K2tVQ0hGR2EzVDhDUUMxZWNFMlpoWlpBWDI3SnlFTUIxajFRYURrNQ0KdDZTZlQ0NHhaa1l1TzN0Mm5COTQxa25NSmR3YnlJK0pVQTJyZi9tR0tyZnI0d3NPQktDcXExT283NmtDUUNpVA0KSm9yZXVwK3hvSHk1MFlGTjJVOW5xRFdwK3pldEVGcDRFVzMzWlFZU2NDQzUrUWx5Rk5UUE9RRGlrV2x1bFFsbA0KaFdjaThLcFNjWVh2dTY4b0NZTUNRSGl2U2JINk1DTHRsc2hSdEgyblNkRi9DcXE4eUpHVXRYWldmRkNMN3I1QQ0KYlFISDMrckFscmJITFZKeVBSQ0VsRkxsUnZZQkhXbVIwWXNtZ1VlYXlCaz0NCi0tLS0tRU5EIFJTQSBQUklWQVRFIEtFWS0tLS0t";//readData("/apps/rsa/private.key").trim();

            byte[] decodedKey           = decoder.decodeBuffer(b64PublicKey);
            byte[] encodedStr           = plaintext.getBytes();//decoder.decodeBuffer(plaintext);
            PublicKey publicKey       = strToPublicKey(new String(decodedKey));

            Cipher cipher               = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);


            byte[] plainText            = cipher.doFinal(encodedStr);

            String text =  encoder.encode(plainText);
            return text;
        }
        catch( Exception e )
        {
           // e.printStackTrace();
            OneCheckoutLogger.log("Error: " + e.getMessage());
            return null;
        }

    }


    public static String Decrypt(String encrypted, String privateKeyString)   {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        try {
            BASE64Decoder decoder   = new BASE64Decoder();
            String b64PrivateKey    = privateKeyString;//"LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQ0KTUlJQ1hBSUJBQUtCZ1FDMGZHNUFTTC9POWh4d1BHNXFTWUQxOVNWMkhqVENuRGV4SG45d25sU3ZaM2trMWpnYg0KQ01CVXpvUXZrUCtIVlJKci8zL0JCV29LdGovS0FockRObVA0cForVW5JcGs1SDdPWmx0N25ETzdib2dQSXhhVA0KN2lVd1J0UjF3MTNvQlE5VGYySCtPa3BuQnVPR3RVZSs4SUg4WVoxQ3FqTDI0cnp5emIrTHp2andiUUlEQVFBQg0KQW9HQkFLakkxTEg1VnFiTk1ic2tTSDNOVDNTMFZOd3BVMTNMaWFseHcrd2xwVHlEbjU3N1ZteXh1bXVVMWJsRg0KK0RFdk5aTVoxUGRGZ08yVGtnUHdBK2NiTGxEL1JvM3lEQ3Z1RGNzS25XQys2Sm1Jc1prK0NScDNGMW16dGNweA0KOVVldnUxejNYd0JXUis0QkhIaGFaZUFxTnhNU3FrL1FhWjV6WjJRS21CSG1SSlZoQWtFQTJFWkU1ZVBua0s3Kw0KUlhENVQ2bDVLei92ZnJ1UTF2Q280ZG9ZdUlGVm5QblBacWYrUXU1OWIySXVpaUNkdkx1bm0ybWZTT1M0UUt2bQ0KOHZQSTJkNkxVd0pCQU5XalRsZEZzUnptN3F0S08xYkNmd0g1U1lPSC9PaHFTcnRMSzhuTlFyQS85OWFVYUhndA0KTm1DZ052VXhndWtHdkRDQ1NnT3JBeUd3K2tVQ0hGR2EzVDhDUUMxZWNFMlpoWlpBWDI3SnlFTUIxajFRYURrNQ0KdDZTZlQ0NHhaa1l1TzN0Mm5COTQxa25NSmR3YnlJK0pVQTJyZi9tR0tyZnI0d3NPQktDcXExT283NmtDUUNpVA0KSm9yZXVwK3hvSHk1MFlGTjJVOW5xRFdwK3pldEVGcDRFVzMzWlFZU2NDQzUrUWx5Rk5UUE9RRGlrV2x1bFFsbA0KaFdjaThLcFNjWVh2dTY4b0NZTUNRSGl2U2JINk1DTHRsc2hSdEgyblNkRi9DcXE4eUpHVXRYWldmRkNMN3I1QQ0KYlFISDMrckFscmJITFZKeVBSQ0VsRkxsUnZZQkhXbVIwWXNtZ1VlYXlCaz0NCi0tLS0tRU5EIFJTQSBQUklWQVRFIEtFWS0tLS0t";//readData("/apps/rsa/private.key").trim();

            String b64EncryptedStr  = encrypted;//"W30Z/nmZKDZqyqhTRBzx29QYiJ+uqbCDvo+7ctEHnuueWDs3OmTk1X4HTEia4ziz7B+idbzyAGstHcv3K2/ZIDjnuKoUtcHWhushaMhaKWyl0iqFHLD0LVz7RhL8Ki6ol6GEFVytpld0iwU3XvulEU8HnkvnMskuCo3UnSLxtUY=";


            byte[] decodedKey           = decoder.decodeBuffer(b64PrivateKey);
            byte[] decodedStr           = decoder.decodeBuffer(b64EncryptedStr);
            PrivateKey privateKey       = strToPrivateKey(new String(decodedKey));

            Cipher cipher               = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);


            byte[] plainText            = cipher.doFinal(decodedStr);

            String text =  new String(plainText);
            return text;
        }
        catch( Exception e )
        {
           // e.printStackTrace();
            OneCheckoutLogger.log("Error: " + e.getMessage());
            return null;
        }

    }


    public static PrivateKey strToPrivateKey(String s)
    {
        try {
            BufferedReader br   = new BufferedReader( new StringReader(s) );
            PEMReader pr        = new PEMReader(br);
            KeyPair kp          = (KeyPair)pr.readObject();
            pr.close();
            return kp.getPrivate();
        }
        catch( Exception e )        {
            OneCheckoutLogger.log("OneCheckoutRSA.strToPrivateKey : Error Convert %s", e.getMessage());
            return null;
        }

    }
    

    public static PublicKey strToPublicKey(String s)
    {
        try {
            BufferedReader br   = new BufferedReader( new StringReader(s) );
            PEMReader pr        = new PEMReader(br);
            JCERSAPublicKey JCEKey = (JCERSAPublicKey) pr.readObject();

            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(JCEKey.getModulus(), JCEKey.getPublicExponent());
            KeyFactory fact = KeyFactory.getInstance("RSA");
            PublicKey pubKey = fact.generatePublic(keySpec);


            pr.close();
            return pubKey;
        }
        catch( Exception e )        {
            e.printStackTrace();
            OneCheckoutLogger.log("OneCheckoutRSA.strToPublicKey : Error Convert %s", e.getMessage());
            return null;
        }


    }
}
