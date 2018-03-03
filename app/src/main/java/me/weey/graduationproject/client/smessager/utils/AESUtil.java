package me.weey.graduationproject.client.smessager.utils;

import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class AESUtil {

    private static boolean initialized = false;

    private static final String ALGORITHM = "AES/ECB/PKCS7Padding";

    /**
     * AES加密
     * @param str  要被加密的字符串
     * @param key  加/解密要用的长度为32的字节数组（256位）密钥
     * @return  加密后的字节数组
     */
    public static byte[] Aes256Encode(byte[] str, byte[] key){
        initialize();
        byte[] result = null;
        try{
            Cipher cipher = Cipher.getInstance(ALGORITHM, "SC");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES"); //生成加密解密需要的Key
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            result = cipher.doFinal(str);
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @param bytes  要被解密的字节数组
     * @param key    加/解密要用的长度为32的字节数组（256位）密钥
     * @return String  解密后的字符串
     */
    public static byte[] Aes256Decode(byte[] bytes, byte[] key){
        initialize();
        try{
            Cipher cipher = Cipher.getInstance(ALGORITHM, "SC");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES"); //生成加密解密需要的Key
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return cipher.doFinal(bytes);
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static void initialize(){
        if (initialized) return;
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
        initialized = true;
    }
}