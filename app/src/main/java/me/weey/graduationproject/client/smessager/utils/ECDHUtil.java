package me.weey.graduationproject.client.smessager.utils;

import android.text.TextUtils;

import com.vondear.rxtools.RxEncodeTool;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.Base64;

/**
 * 生成用于ECDH加密相关的工具类
 * Created by WeiKai on 2018/01/23.
 */
public class ECDHUtil {

    /**
     * 根据传来的公钥ECDH加密信息，参数只要任选一个传参即可
     * @param message          要加密的内容，要求必须以utf8编码
     * @param BASE64PublicKey 传来的是经过BASE64编码后的字节数组
     * @param rawPublicKey    传来的是原生PublicKey字节数组
     * @return 返回经过公钥加密后再经过BASE64编码后的内容
     */
    public static String encryption(byte[] message, String BASE64PublicKey, byte[] rawPublicKey) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        if (message == null || message.length == 0) return "";
        byte[] bytePublicKey;
        //判断是哪个参数
        if (!TextUtils.isEmpty(BASE64PublicKey)) {
            //表示传来的是base64字符串
            bytePublicKey =  RxEncodeTool.base64Decode(BASE64PublicKey.getBytes("UTF-8"));
        } else {
            bytePublicKey = rawPublicKey;
        }
        //非空判断
        if (bytePublicKey == null || bytePublicKey.length == 0) return "";
        //转换成PublicKey
        PublicKey publicKey = KeyUtil.toPublicKey(bytePublicKey);
        //加密信息
        Cipher iesCipher = Cipher.getInstance("ECIES", BouncyCastleProvider.PROVIDER_NAME);
        iesCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        //获取加密后的原始内容
        byte[] rawData = iesCipher.doFinal(message);
        //返回BASE64编码
        return RxEncodeTool.base64Encode2String(rawData);
    }

    /**
     * 根据传来的加密数据和私钥信息，解密
     * @param BASE64EncryptionMessage 已经BASE64后的加密信息
     * @param BASE64PrivateKey        BASE64编码后的字节数组组成的私钥
     * @param rawPrivateKey           原生私钥的字节数组
     * @return                        解密后的原始数据
     */
    public static byte[] decrypt(String BASE64EncryptionMessage, String BASE64PrivateKey, byte[] rawPrivateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        if (BASE64EncryptionMessage == null || BASE64EncryptionMessage.isEmpty()) return null;
        byte[] bytePrivateKey;
        //判断是哪个参数
        if (!TextUtils.isEmpty(BASE64PrivateKey)) {
            //表示传来的是base64字符串
            bytePrivateKey = RxEncodeTool.base64Decode(BASE64PrivateKey.getBytes("UTF-8"));
        } else {
            bytePrivateKey = rawPrivateKey;
        }
        //非空判断
        if (bytePrivateKey == null || bytePrivateKey.length == 0) return null;
        //转换成PrivateKey
        PrivateKey privateKey = KeyUtil.toPrivateKey(bytePrivateKey);
        //解密信息
        Cipher jie = Cipher.getInstance("ECIES", "SC");
        jie.init(Cipher.DECRYPT_MODE, privateKey);
        //将加密信息Base64解码
        return jie.doFinal(RxEncodeTool.base64Decode(BASE64EncryptionMessage.getBytes("UTF-8")));
    }
}
