package me.weey.graduationproject.client.smessager.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.vondear.rxtools.RxDataTool;
import com.vondear.rxtools.RxEncodeTool;
import com.vondear.rxtools.RxEncryptTool;
import com.vondear.rxtools.RxFileTool;
import com.vondear.rxtools.RxTimeTool;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.activity.ChatActivity;
import me.weey.graduationproject.client.smessager.entity.ChatMessage;
import me.weey.graduationproject.client.smessager.entity.DataStructure;
import me.weey.graduationproject.client.smessager.entity.HttpResponse;
import me.weey.graduationproject.client.smessager.entity.Msg;
import me.weey.graduationproject.client.smessager.entity.User;
import me.weey.graduationproject.client.smessager.sqlite.ChatListOpenHelper;
import me.weey.graduationproject.client.smessager.utils.AESUtil;
import me.weey.graduationproject.client.smessager.utils.CommonUtil;
import me.weey.graduationproject.client.smessager.utils.Constant;
import me.weey.graduationproject.client.smessager.utils.ECDHUtil;
import me.weey.graduationproject.client.smessager.utils.ECDSAUtil;
import me.weey.graduationproject.client.smessager.utils.KeyUtil;
import me.weey.graduationproject.client.smessager.utils.SPUtil;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * 保持Login通信的Service
 * Created by weikai on 2018/02/04/0004.
 */

public class LoginHandlerService extends Service {

    //登录相关的常量
    public static final String LOGIN_ACTIVITY_HANDLER = "mainHandler";
    public static final String CHAT_ACTIVITY_HANDLER = "chatHandler";
    public static final int ESTABLISH_CONNECTION_SUCCESS= 888880;
    public static final int RECEIVE_NEW_MESSAGE = 888881;

    //获取好友列表相关的常量
    public static final int GET_FRIENDS_LIST = 10101;  //获取好友列表成功

    private static WebSocket mLoginSocket;
    private Messenger mMessager;
    private Messenger mChatMessenger;

    private User mMyUser;
    private final static Type type = new TypeReference<HashMap<String, String>>() {}.getType();


    public LoginHandlerService() {

    }

    /**
     * 服务被startService启动
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //获取LoginActivity的Handler
        if (intent.getExtras() != null) {
            Object mainHandler = intent.getExtras().get(LOGIN_ACTIVITY_HANDLER);
            Object chatHandler = intent.getExtras().get(CHAT_ACTIVITY_HANDLER);
            if (mainHandler != null) {
                mMessager = (Messenger) mainHandler;
            }
            if (chatHandler != null) {
                mChatMessenger = (Messenger) chatHandler;
            }
        }

        //取出当前用户的信息
        if (mMyUser == null)
            mMyUser = JSON.parseObject(SPUtil.getString(getApplicationContext(), Constant.USER_INFO), User.class);

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * 在每次开启服务的时候获取传来的封装数据信息建立与服务器的Socket通信
     */
    public void socketLogin(final String dataStructure) {
        if (mLoginSocket != null) {
            //已经有连接了，就直接返回给handler成功的信息
            Message obtain = Message.obtain();
            obtain.what = Constant.CODE_SUCCESS;
            try {
                mMessager.send(obtain);
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (TextUtils.isEmpty(dataStructure)) return;
        //构建请求体
        Request request = new Request.Builder()
                .url(Constant.SERVER_ADDRESS + "user")
                .build();
        //建立连接
        CommonUtil.getHttpClient().newWebSocket(request, new WebSocketListener() {
            //Socket连接建立
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Constant.isOnLine = true;
                //这个连接建立了以后就长时间保持了，因此需要设置成全局变量
                mLoginSocket = webSocket;
                //把登录信息发送给服务器
                mLoginSocket.send(dataStructure);
                System.out.println("client onOpen");
                System.out.println("client request header:" + response.request().headers());
                System.out.println("client response header:" + response.headers());
                System.out.println("client response:" + response);
            }

            //接收到字符串类型的字符数据
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                System.out.println("client onMessage");
                System.out.println("message:" + text);
                //收到信息，转成HttpResponse对象
                HttpResponse httpResponse = JSON.parseObject(text, HttpResponse.class);
                if (httpResponse == null) return;
                Message obtain = Message.obtain();
                //对返回的数据校验
                switch (httpResponse.getMessageType()) {
                    case Constant.MESSAGE_TYPE_LOGIN:
                        //表示这是对LOGIN方面的数据的回应，那回传回来的应该是包含用户id的User对象
                        try {
                            //回传信息给Activity
                            obtain.what = httpResponse.getStatusCode();
                            obtain.obj = httpResponse.getMessage();
                            mMessager.send(obtain);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                    case Constant.MESSAGE_TYPE_GET_FRIENDS_LIST:
                        //表示这是请求好友列表的回应
                        try {
                            String message = httpResponse.getMessage();
                            obtain.what = GET_FRIENDS_LIST;
                            obtain.obj = message;
                            mMessager.send(obtain);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                    case Constant.MESSAGE_TYPE_IS_ONLINE:
                        //对是否在线的回应
                        handleIsOnline(httpResponse.getMessage());
                        break;
                    case Constant.MESSAGE_TYPE_SIGNATURE:
                        //签名相关的
                        if (httpResponse.getStatusCode().equals(Constant.CODE_SUCCESS)) {
                           handleSignature(httpResponse.getMessage());
                        }
                        break;
                    case Constant.MESSAGE_TYPE_SEND_MESSAGE:
                        //收到其他客户端发送来的消息
                        if (httpResponse.getStatusCode().equals(Constant.CODE_FAILURE)) {
                            try {
                                obtain.what = Constant.CODE_FAILURE;
                                obtain.obj = httpResponse.getMessage();
                                mMessager.send(obtain);
                                return;
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        //正确的消息需要额外处理
                        handleMessage(httpResponse.getMessage());
                        break;
                }
            }

            //接收到二进制类型的字节数据
            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);
            }

            //连接正在准备关闭
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Constant.isOnLine = false;
                Message obtain = Message.obtain();
                obtain.what = Constant.CODE_CONNECTION_LOST;
                obtain.obj = getResources().getString(R.string.connection_lost);
                try {
                    mMessager.send(obtain);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                super.onClosing(webSocket, code, reason);
            }

            //连接已经关闭
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Constant.isOnLine = false;
                Message obtain = Message.obtain();
                obtain.what = Constant.CODE_CONNECTION_LOST;
                obtain.obj = getResources().getString(R.string.connection_lost);
                try {
                    mMessager.send(obtain);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mLoginSocket = null;
                super.onClosed(webSocket, code, reason);
            }

            //通信失败的时候
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
                Constant.isOnLine = false;
                Message obtain = Message.obtain();
                //对返回的数据校验
                obtain.what = Constant.CODE_FAILURE;
                obtain.obj = getResources().getString(R.string.http_failure);
                try {
                    mMessager.send(obtain);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mLoginSocket = null;
                super.onFailure(webSocket, t, response);
            }
        });
    }

    /**
     * 加密流程一回应：处理对是否在线的回应
     */
    private void handleIsOnline(String message) {
        if (mChatMessenger == null) return;
        //转为消息类型
        DataStructure dataStructure = JSON.parseObject(message, DataStructure.class);
        //获取要聊天的对象
        User friendUser = null;
        for (User u : Constant.getFriendsListInstant()) {
            if (dataStructure.getToID().equals(u.getId())) {
                friendUser = u;
                break;
            }
        }
        if (friendUser == null) {
            //表示没有获取到这个好友的信息
            try {
                Message obtain = Message.obtain();
                obtain.what = Constant.CODE_PROCESS_FAILURE;
                obtain.obj = "没有相关好友的信息，请重试！";
                mChatMessenger.send(obtain);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return;
        }
        if (dataStructure.getMessage().equals("true")) {
            //流程进到二
            Constant.getProcessMapInstant().put(friendUser.getId(), 2);
            //流程的第二步，生成新的密钥对，然后发送给服务器
            sendPublicKey(friendUser);
        } else {
            //不在线
            try {
                Message obtain = Message.obtain();
                obtain.what = Constant.CODE_PROCESS_FAILURE;
                obtain.obj = "好友此时不在线，请等好友在线后再发起加密聊天吧！";
                mChatMessenger.send(obtain);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            //流程回归0
            Constant.getProcessMapInstant().put(friendUser.getId(), 0);
        }
    }

    /**
     * 加密流程二：生成新的密钥对，然后发送给服务器
     * @param friendUser 要聊天的好友的相关信息
     */
    private void sendPublicKey(User friendUser) {

        PublicKey publicKey = null;
        PrivateKey privateKey = null;

        if (TextUtils.isEmpty(Constant.getMyPublicKeyMapInstant().get(friendUser.getId()))
                || TextUtils.isEmpty(Constant.getMyPrivateKeyMapInstant().get(friendUser.getId()))) {
            try {
                //不存在公私钥，就生成
                KeyPair keyPair = KeyUtil.generateKey();
                privateKey = keyPair.getPrivate();
                publicKey = keyPair.getPublic();
                //存储信息
                Constant.getMyPrivateKeyMapInstant().put(friendUser.getId(), RxEncodeTool.base64Encode2String(keyPair.getPrivate().getEncoded()));
                Constant.getMyPublicKeyMapInstant().put(friendUser.getId(), RxEncodeTool.base64Encode2String(keyPair.getPublic().getEncoded()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            //存在公私钥
            publicKey = KeyUtil.toPublicKey(RxEncodeTool.base64Decode(Constant.getMyPublicKeyMapInstant().get(friendUser.getId())));
            privateKey = KeyUtil.toPrivateKey(RxEncodeTool.base64Decode(Constant.getMyPrivateKeyMapInstant().get(friendUser.getId())));
        }
        if (publicKey == null || privateKey == null) return;

        //发送公钥给服务器
        sendMessage(friendUser.getId(), RxEncodeTool.base64Encode2String(publicKey.getEncoded()), Constant.MESSAGE_TYPE_SIGNATURE, Constant.MODEL_TYPE_CHAT, 2);
    }

    /**
     * 加密流程二回应：对服务器端的签名进行校验
     */
    private void handleSignature(String message) {
        //转为消息类型
        DataStructure dataStructure = JSON.parseObject(message, DataStructure.class);
        //获取要聊天的对象
        User friendUser = null;
        for (User u : Constant.getFriendsListInstant()) {
            if (dataStructure.getToID().equals(u.getId())) {
                friendUser = u;
                break;
            }
        }
        if (friendUser == null) {
            //表示没有获取到这个好友的信息
            try {
                Message obtain = Message.obtain();
                obtain.what = Constant.CODE_PROCESS_FAILURE;
                obtain.obj = "没有相关好友的信息，请重试！";
                mChatMessenger.send(obtain);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return;
        }
        //签名相关的回应
        if (Constant.getProcessMapInstant().get(friendUser.getId()) != 2) {
            try {
                Message obtain = Message.obtain();
                obtain.what = Constant.CODE_PROCESS_FAILURE;
                obtain.obj = "加密流程错误！请重新开始聊天！";
                mChatMessenger.send(obtain);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return;
        }
        //将JSON转换
        HashMap<String, String> sign = null;
        try {
            sign = JSON.parseObject(dataStructure.getMessage(), type);
        } catch (Exception e) {
            e.printStackTrace();
            Constant.getProcessMapInstant().put(friendUser.getId(), 0);
            try {
                Message obtain = Message.obtain();
                obtain.what = Constant.CODE_PROCESS_FAILURE;
                obtain.obj = "获取服务器签名信息失败！";
                mChatMessenger.send(obtain);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
            return;
        }
        //对签名进行校验
        //获取签名信息
        String signature = sign.get("signature");
        String publicKeyA = sign.get("publicKey");
        //先对publicKey校验
        String publicKey = Constant.getMyPublicKeyMapInstant().get(friendUser.getId());
        if (!publicKey.equals(publicKeyA)) {
            //校验不相等
            Constant.getProcessMapInstant().put(friendUser.getId(), 0);
            try {
                Message obtain = Message.obtain();
                obtain.what = Constant.CODE_PROCESS_FAILURE;
                obtain.obj = "publicKey校验失败！";
                mChatMessenger.send(obtain);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
            return;
        }
        //对签名进行校验
        boolean isTrue = false;
        try {
            isTrue = ECDSAUtil.verifySignature(publicKey.getBytes("UTF-8"), signature, Constant.SERVER_PUBLIC_KEY, null);
            if (!isTrue) {
                throw new SignatureException();
            }
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            //校验失败
            Constant.getProcessMapInstant().put(friendUser.getId(), 0);
            e.printStackTrace();
            try {
                Message obtain = Message.obtain();
                obtain.what = Constant.CODE_PROCESS_FAILURE;
                obtain.obj = "流程二对服务器回传的签名校验失败！";
                mChatMessenger.send(obtain);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
            return;
        }


        //校验成功，进入流程三
        Constant.getProcessMapInstant().put(friendUser.getId(), 3);
        randomSend(signature, friendUser);
    }

    /**
     * 加密流程三：发送校验信息给客户端B
     * @param signature 服务器私钥对publicKeyA的签名信息
     * @param friendUser 好友对象的信息
     */
    private void randomSend(String signature, User friendUser) {
        try {
            //生成随机数放到map中
            int randomNum = new Random().nextInt();
            String randomNumString = randomNum + "";
            Constant.getfriendRandomMapInstant().put(friendUser.getId(), randomNum);
            //签名
            String signatureRandom = ECDSAUtil.signature(randomNumString.getBytes("UTF-8"), Constant.getMyPrivateKeyMapInstant().get(friendUser.getId()), null);
            //封装信息发送
            HashMap<String, String> map = new HashMap<>();
            map.put("publicKeyA", Constant.getMyPublicKeyMapInstant().get(friendUser.getId()));
            map.put("sigA", signature);
            map.put("sigRandomA", signatureRandom);

            sendMessage(friendUser.getId(), JSON.toJSONString(map), Constant.MESSAGE_TYPE_SEND_MESSAGE, Constant.MODEL_TYPE_CHAT, 3);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | SignatureException | InvalidKeyException | UnsupportedEncodingException e) {
            e.printStackTrace();
            try {
                Message obtain = Message.obtain();
                obtain.what = Constant.CODE_PROCESS_FAILURE;
                obtain.obj = "加密流程三发送错误，错误信息：" + e.getMessage();
                mChatMessenger.send(obtain);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * 收到另外一个客户端发来消息的时候对消息的处理
     */
    private void handleMessage(String message) {
        final DataStructure dataStructure;
        try {
            dataStructure = JSON.parseObject(message, DataStructure.class);
            //取出当前用户的信息
            if (mMyUser == null)
            mMyUser = JSON.parseObject(SPUtil.getString(getApplicationContext(), Constant.USER_INFO), User.class);
        } catch (Exception e) {
            return;
        }

        if (dataStructure == null || mMyUser == null || !dataStructure.getToID().equals(mMyUser.getId())) {
            return;
        }
        User friendUser = null;
        //获取用户的信息
        ArrayList<User> friendsListInstant = Constant.getFriendsListInstant();
        for (User u : friendsListInstant) {
            if (u.getId().equals(dataStructure.getFromId())) {
                friendUser = u;
                break;
            }
        }
        if (friendUser == null) return;

        switch (dataStructure.getProcess()) {
            case 3:
                try {
                    HashMap<String, String> map = JSON.parseObject(dataStructure.getMessage(), type);
                    String publicKeyA = map.get("publicKeyA");
                    String sigA = map.get("sigA");
                    String sigRandomA = map.get("sigRandomA");
                    //利用服务器公钥校验publicKeyA
                    boolean isTrue = ECDSAUtil.verifySignature(publicKeyA.getBytes("UTF-8"), sigA, Constant.SERVER_PUBLIC_KEY, null);
                    if (!isTrue) return;
                    //保存publicKeyA
                    Constant.getFriendPublicKeyMapInstant().put(friendUser.getId(), publicKeyA);
                    //用publicKeyA对sigRandomA进行加密
                    String encryptionMsg = ECDHUtil.encryption(sigRandomA.getBytes("UTF-8"), publicKeyA, null);
                    //发送消息
                    sendMessage(friendUser.getId(), encryptionMsg, Constant.MESSAGE_TYPE_SEND_MESSAGE, Constant.MODEL_TYPE_CHAT, 4);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //判断ChatActivity是否在前台
                if (ChatActivity.isFront) {
                    //在前台说明此时用户的身份是发起聊天者
                    if (Constant.getProcessMapInstant().get(friendUser.getId()).equals(5)) {
                        //校验成功以后生成AES密钥
                        byte[] aes256Key = KeyUtil.generateAES256Key();
                        //把aes密钥转为BASE64后存到Map中
                        Constant.getAesKeyMapInstant().put(friendUser.getId(), RxEncodeTool.base64Encode2String(aes256Key));
                        //把密钥用B的publicKey加密后发送给B
                        String bPublicKey = Constant.getFriendPublicKeyMapInstant().get(friendUser.getId());
                        if (!TextUtils.isEmpty(bPublicKey)) {
                            //加密
                            try {
                                String encryption = ECDHUtil.encryption(aes256Key, bPublicKey, null);
                                sendMessage(friendUser.getId(), encryption, Constant.MESSAGE_TYPE_SEND_MESSAGE, Constant.MODEL_TYPE_CHAT, 6);
                                Constant.getProcessMapInstant().put(friendUser.getId(), 6);
                                //给Activity发送消息，建立连接成功！
                                Message obtain = Message.obtain();
                                obtain.what = ESTABLISH_CONNECTION_SUCCESS;
                                mChatMessenger.send(obtain);
                                //流程改为聊天中
                                Constant.getProcessMapInstant().put(friendUser.getId(), 7);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }

                } else {
                    //不在前台，那就说明这个用户是被动接收其他人的聊天请求
                    /*那就先校验完成对方的流程三，对方的流程三完成后自己这边再发送通知，然后如果通知点击来以后就开始客户端B这边的流程建立*/
                             //解析JSON

                    //弹出通知
                    Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                    intent.putExtra(ChatActivity.CHAT_INFO, friendUser);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    CommonUtil.showNotification(getApplicationContext(), BitmapFactory.decodeResource(getResources(), R.mipmap.paper_plane),
                            R.mipmap.ic_message_white_24dp, "您有新的聊天请求!", "您有新的聊天请求!", friendUser.getUserName() + " 请求与您聊天！",
                            pendingIntent, null, 1);
                }
                break;
            case 4:
                //流程四，验证对方是否已经拿到自己的publicKey
                try {
                    //使用自己的私钥解密
                    byte[] decrypt = ECDHUtil.decrypt(dataStructure.getMessage(), Constant.getMyPrivateKeyMapInstant().get(friendUser.getId()), null);
                    String sigRandomA = new String(decrypt, "UTF-8");
                    //获取随机数
                    String random = Constant.getfriendRandomMapInstant().get(friendUser.getId()) + "";
                    //签名校验
                    boolean b = ECDSAUtil.verifySignature(random.getBytes("UTF-8"), sigRandomA, Constant.getMyPublicKeyMapInstant().get(friendUser.getId()), null);
                    if (!b) {
                        //校验失败
                        Constant.getProcessMapInstant().put(friendUser.getId(), 0);
                    } else {
                        Constant.getProcessMapInstant().put(friendUser.getId(), 5);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case 6:
                try {
                    //是客户端B这边接收AES密钥
                    String aesString = dataStructure.getMessage();
                    //使用自己的私钥解密
                    String privateKey = Constant.getMyPrivateKeyMapInstant().get(friendUser.getId());
                    byte[] aesKey = ECDHUtil.decrypt(aesString, privateKey, null);
                    //存入map
                    Constant.getAesKeyMapInstant().put(friendUser.getId(), RxEncodeTool.base64Encode2String(aesKey));
                    //给activity发送建立连接成功的通知
                    Message obtain = Message.obtain();
                    obtain.what = ESTABLISH_CONNECTION_SUCCESS;
                    mChatMessenger.send(obtain);
                    //流程改为聊天中
                    Constant.getProcessMapInstant().put(friendUser.getId(), 7);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 7:
                //接收到正常聊天的信息 todo:给服务器端发送重新建立通信的通知
                if (!Constant.getProcessMapInstant().get(friendUser.getId()).equals(7)) {
                    try {
                        Message obtain = Message.obtain();
                        obtain.what = Constant.CODE_PROCESS_FAILURE;
                        obtain.obj = "收到错误的信息！";
                        mChatMessenger.send(obtain);
                    } catch (RemoteException e1) {
                        e1.printStackTrace();
                    }
                    return;
                }

                //判断此时有没有在前台，在前台就直接把信息传给Activity处理，如果在后台，发送通知，把消息存储到数据库
                handleReceiveMsg(dataStructure, friendUser);
                break;
        }
    }

    /**
     * 收到普通聊天信息时候的处理
     */
    private void handleReceiveMsg(DataStructure dataStructure, User friendUser) {
        //首先，需要用aes密码解密
        String aesKeyBase64 = Constant.getAesKeyMapInstant().get(friendUser.getId());
        byte[] aesKeyDecode = RxEncodeTool.base64Decode(aesKeyBase64);
        //获取信息
        Msg msg = JSON.parseObject(dataStructure.getMessage(), Msg.class);
        if (msg == null) return;

        String receiveMsg = null;
        String voiceSecond = "";

        if (msg.getMsgType() > 0) {
            //只处理语音和图片
            try {
                //图片，保存到本地
                receiveMsg = getCacheDir() + "/media/" + UUID.randomUUID().toString();
                //解密图片/语音
                byte[] aes256Decode = AESUtil.Aes256Decode(msg.getMessage(), aesKeyDecode);
                if (aes256Decode == null) return;
                //把字节数组组成的图片或者语音保存到本地
                File decodeFile = CommonUtil.getFileFromBytes(aes256Decode, receiveMsg);
                if (!decodeFile.exists()) return;
                //如果是语音信息，那么要把消息类型改成未读语音的类型
                if (msg.getMsgType() == Constant.CHAT_MESSAGE_TYPE_VOICE_HAVE_LISTEN) {
                    msg.setMsgType(Constant.CHAT_MESSAGE_TYPE_VOICE_NEW);
                    //获取声音长度
                    int amrDuration = CommonUtil.getAmrDuration(decodeFile);
                    if (amrDuration < 1) voiceSecond = "1";
                    else voiceSecond = amrDuration + "";
                }
                //存储到数据库
                saveMessageToDB(receiveMsg, msg.getMsgType(), RxTimeTool.date2String(dataStructure.getTime()), friendUser.getId(), false, voiceSecond);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (msg.getMsgType() == Constant.CHAT_MESSAGE_TYPE_TEXT){
            //处理文字信息
            try {
                byte[] bytes = AESUtil.Aes256Decode(msg.getMessage(), aesKeyDecode);
                if (bytes != null)
                receiveMsg = new String(bytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //存储到数据库
            if (receiveMsg != null) {
                saveMessageToDB(receiveMsg, msg.getMsgType(), RxTimeTool.date2String(dataStructure.getTime()), friendUser.getId(), false, "");
            }
        }


        if (ChatActivity.isFront) {
            try {
                //在前台，把消息发送给前台
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setTime(RxTimeTool.date2String(dataStructure.getTime()));
                chatMessage.setUserId(friendUser.getId());
                chatMessage.setChatType(1);
                chatMessage.setMessage(receiveMsg);
                chatMessage.setMessageType(msg.getMsgType());
                chatMessage.setVoiceSecond(voiceSecond);

                Message obtain = Message.obtain();
                obtain.what = RECEIVE_NEW_MESSAGE;
                obtain.obj = chatMessage;
                mChatMessenger.send(obtain);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            //todo 不在前台，弹出通知
        }
    }

    /**
     * 保存消息到数据库
     */
    public void saveMessageToDB(String msg, Integer msgType, String date, String userID, Boolean isSend, String voiceSecond) {
        //把消息都保存到数据库
        ChatListOpenHelper chatListOpenHelper = new ChatListOpenHelper(getApplicationContext(), Constant.CHAT_LIST_DB_NAME, null, 1);
        //新建一个数据库的连接
        SQLiteDatabase readableDatabase = chatListOpenHelper.getReadableDatabase();
        //启用事务
        readableDatabase.beginTransaction();
        ContentValues message = new ContentValues();
        message.put("id", UUID.randomUUID().toString());
        message.put("user_id", userID);
        message.put("message", msg);
        message.put("time", date);
        if (isSend) {
            //属于发送
            message.put("chat_type", 0);
        } else {
            message.put("chat_type", 1);
        }
        message.put("message_type", msgType);
        message.put("voice_second", voiceSecond);
        readableDatabase.insert("chat_message", null, message);

        //设置事务标志为成功，当结束事务时就会提交事务
        readableDatabase.setTransactionSuccessful();

        //关闭事务
        readableDatabase.endTransaction();
        readableDatabase.close();
        chatListOpenHelper.close();
    }


    /**
     * 暴露外部接口给Activity调用发送请求
     */
    public void sendMessage(String toID, String message, int messageType, int modelType, int process) {
        DataStructure dataStructure = new DataStructure();
        dataStructure.setFromId(mMyUser.getId());
        dataStructure.setToID(toID);
        dataStructure.setMessage(message);
        dataStructure.setMessageType(messageType);
        dataStructure.setTime(new Date());
        dataStructure.setModelType(modelType);
        dataStructure.setProcess(process);
        if (mLoginSocket == null) {
            return;
        }
        mLoginSocket.send(JSON.toJSONString(dataStructure));
    }

    /**
     * 加密流程一：开始加密聊天的流程
     * @param friendUser 对应好友的对象
     */
    public void startChatProcess(User friendUser) {
        sendMessage(friendUser.getId(), "", Constant.MESSAGE_TYPE_IS_ONLINE, Constant.MODEL_TYPE_CHAT, 1);

        //取出当前用户的信息
        if (mMyUser == null)
            mMyUser = JSON.parseObject(SPUtil.getString(getApplicationContext(), Constant.USER_INFO), User.class);

        //更新好友对应的流程
        Constant.getProcessMapInstant().put(friendUser.getId(), 1);
    }

    /**
     * 回调函数
     */
    private onReceiveMessageCallback mOnReceiveMessageCallback=null;

    public void setOnDataCallback(onReceiveMessageCallback mOnReceiveMessageCallback) {
        this.mOnReceiveMessageCallback = mOnReceiveMessageCallback;
    }

    public interface onReceiveMessageCallback{
        void onReceiveMessage(String message);
    }

    /**
     * 使用Bind方式通信
     */
    private final IBinder mBinder = new LoginBinder();

    public class LoginBinder extends Binder {
        public LoginHandlerService getService() {
            return LoginHandlerService.this;
        }
    }
}
