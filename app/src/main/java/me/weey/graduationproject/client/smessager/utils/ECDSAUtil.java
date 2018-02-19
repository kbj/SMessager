package me.weey.graduationproject.client.smessager.utils;


import android.text.TextUtils;

import com.vondear.rxtools.RxEncodeTool;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * 关于ECDSA签名的工具类
 * Created by WeiKai on 2018/01/23.
 */
public class ECDSAUtil {

    /**
     * 利用私钥对原数据签名
     * @param message            要签名的数据
     * @param BASE64PrivateKey   用来签名数据的BASE64后的私钥
     * @param rawPrivateKey      原生的字节数组构成的私钥
     * @return                   返回经过Base64处理签名后的字符串
     */
    public static String signature(byte[] message, String BASE64PrivateKey, byte[] rawPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        //对传来的数据作校验
        if (message == null || message.length == 0) {
            return "";
        }
        byte[] bytePrivateKey;
        if (!TextUtils.isEmpty(BASE64PrivateKey)) {
            //不为空，说明传来的是字符串
            bytePrivateKey = RxEncodeTool.base64Decode(BASE64PrivateKey);
        } else {
            bytePrivateKey = rawPrivateKey;
        }
        //非空校验
        if (bytePrivateKey == null || bytePrivateKey.length == 0) {
            return "";
        }
        PrivateKey privateKey = KeyUtil.toPrivateKey(bytePrivateKey);
        //开始签名
        Signature signature = Signature.getInstance("SHA512withECDSA");
        signature.initSign(privateKey);
        signature.update(message);
        return RxEncodeTool.base64Encode2String(signature.sign());
    }

    /**
     * 使用公钥对传来的签名数据进行校验
     * @param rawMessage            要校验的数据
     * @param signMessage           传来的签名数据
     * @param BASE64PublicKey       签名Base64后的PublicKey
     * @param rawPublicKey          PublicKey的字节数组
     * @return                      是否成功
     */
    public static boolean verifySignature(byte[] rawMessage, String signMessage, String BASE64PublicKey, byte[] rawPublicKey) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        //传入参数校验
        if (rawMessage == null || rawMessage.length == 0 || signMessage == null || signMessage.isEmpty()) {
            return false;
        }
        byte[] bytePublicKey;
        if (!TextUtils.isEmpty(BASE64PublicKey)) {
            bytePublicKey = RxEncodeTool.base64Decode(BASE64PublicKey);
        } else {
            bytePublicKey = rawPublicKey;
        }
        if (bytePublicKey == null || bytePublicKey.length == 0) {
            return false;
        }
        PublicKey publicKey = KeyUtil.toPublicKey(bytePublicKey);
        //校验
        Signature signature = Signature.getInstance("SHA512withECDSA");
        signature.initVerify(publicKey);
        signature.update(rawMessage);
        return signature.verify(RxEncodeTool.base64Decode(signMessage));
    }
}
