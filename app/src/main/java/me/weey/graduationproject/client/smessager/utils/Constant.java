package me.weey.graduationproject.client.smessager.utils;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import me.weey.graduationproject.client.smessager.entity.User;

/**
 * 用到的常量
 * Created by weikai on 2018/01/27/0027.
 */

public class Constant {

    /**
     * 服务端的地址
     */
    public static final String SERVER_ADDRESS = "http://192.168.157.1:8080/";
//    public static final String SERVER_ADDRESS = "http://kpw.free.ngrok.cc/";


    /**
     * 服务器公钥，先暂时写死
     */
    public static final String SERVER_PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEbeVJ3byutOdvdUEsm1BjZJBTHcQZiyBBw9g7f8aBrbMW0VisGosr+/uEFaIsWF37lBjTS3tABpYlgzC39XnDXw==";

    /**
     * 表示服务器的ID对象
     */
    public static final String SERVER_ID = "a0383f4ee0d447718df1c8d053d18823";

    /**
     * 本机IP地址
     */
    public static String IP_ADDRESS = "";

    /**
     * 是否在线
     */
    public static boolean isOnLine = false;


    //-------------------SP中使用的变量-------------------------//
    /**
     * 第一次打开APP
     */
    public static final String FIRST_BOOT = "firstBoot";
    //在移动网络下加载图片
    public static final String LOAD_PICTURE_WITHOUT_WIFI = "loadPictureWithoutWIFI";

    /**
     * 用户信息
     */
    public static final String USER_INFO = "userInfo";



    //-------------------申请运行时权限定义的RequestCode-------------------------//
    /**
     * 获取手机IMEI信息的权限
     */
    public static final int PERMISSION_READ_PHONE_STATE = 2000;

    /**
     * 录音的权限
     */
    public static final int PERMISSION_RECORD_AUDIO = 2001;

    /**
     * 获取外部存储
     */
    public static final int PERMISSION_READ_EXTERNAL_STORAGE = 2002;

    /**
     * --------------------------------------------------------------
     * |--------------------Socket中的ModelType----------------------|
     * --------------------------------------------------------------
     */
    //关于账号方面
    public static final int MODEL_TYPE_ACCOUNT = 1001;
    //聊天方面
    public static final int MODEL_TYPE_CHAT = 1002;

    /**
     * --------------------------------------------------------------
     * |--------------------Socket中的MessageType-------------------|
     * --------------------------------------------------------------
     */
    //登陆的请求
    public static final int MESSAGE_TYPE_LOGIN = 1101;
    //获取这个账号的好友列表
    public static final int MESSAGE_TYPE_GET_FRIENDS_LIST = 1102;
    //判断另外一个ID的好友在线状态
    public static final int MESSAGE_TYPE_IS_ONLINE = 1103;
    //握手的第二步对公钥进行签名
    public static final int MESSAGE_TYPE_SIGNATURE = 1104;
    //握手的第三部以后的信息，发送消息到对应客户端
    public static final int MESSAGE_TYPE_SEND_MESSAGE = 1105;


    /**
     * 返回信息成功状态的状态码
     */
    public static final int CODE_SUCCESS = 200;

    /**
     * 操作失败的校验码
     */
    public static final int CODE_FAILURE = 500;

    /**
     * 校验失败的状态码
     */
    public static final int CODE_CHECK_FAILURE = 501;

    /**
     * 注册时候邮箱已经存在
     */
    public static final int CODE_EMAIL_EXIST = 502;

    /**
     * 注册时候用户名已经存在
     */
    public static final int CODE_USERNAME_EXIST = 503;

    /**
     * 连接断开
     */
    public static final int CODE_CONNECTION_LOST = 504;

    /**
     * 加密流程中断
     */
    public static final int CODE_PROCESS_FAILURE = 505;

    /**
     * 发送的消息
     */
    public static final int CHAT_TYPE_SEND = 0;

    /**
     * 接收的消息
     */
    public static final int CHAT_TYPE_RECEIVE = 1;

    /**
     * 聊天消息的类型
     */
    public static final int CHAT_MESSAGE_TYPE_TEXT = 0;     //文本消息
    public static final int CHAT_MESSAGE_TYPE_IMAGE = 1;    //图片消息
    public static final int CHAT_MESSAGE_TYPE_VOICE_HAVE_LISTEN = 2;    //语音消息已经播放过
    public static final int CHAT_MESSAGE_TYPE_VOICE_NEW = 3;            //新的语音消息未播放

    /**
     * -------------------定义文件的一些路径-------------------------
     */
    //头像缓存的路径
    public static final String AVATAR_CHCHE_DIR = "avatar";

    /**
     * -------------------数据库的名称-------------------------
     */
    //聊天列表界面的数据库
    public static final String CHAT_LIST_DB_NAME = "ChatLists.db";
    //聊天记录的数据库
    public static final String CHAT_MESSAGE_DB_NAME = "ChatMessage.db";

    /**
     * 数据库的集合
     */


    /**
     * 存放全部好友的List集合
     */
    private static final ArrayList<User> friendsList = new ArrayList<>();
    public static ArrayList<User> getFriendsListInstant() {
        return friendsList;
    }

    /**
     * 存放相对应好友的加密流程的Map集合
     */
    private static final ConcurrentHashMap<String, Integer> processMap = new ConcurrentHashMap<String, Integer>();

    public static ConcurrentHashMap<String, Integer> getProcessMapInstant() {
        return processMap;
    }

    /**
     * 存放好友的AES的Key，由谁发起的聊天就是由谁生成的
     */
    private static final ConcurrentHashMap<String, String> aesKeyMap = new ConcurrentHashMap<String, String>();

    public static ConcurrentHashMap<String, String> getAesKeyMapInstant() {
        return aesKeyMap;
    }

    /**
     * 用map存放自己生成的public Key，map的key为好友的id，value为我给这个好友的publicKey
     */
    private static final ConcurrentHashMap<String, String> myPublicKeyMap = new ConcurrentHashMap<String, String>();

    public static ConcurrentHashMap<String, String> getMyPublicKeyMapInstant() {
        return myPublicKeyMap;
    }

    /**
     * 用map存放自己生成的private Key，map的key为好友的id，value为我给这个好友的privateKey
     */
    private static final ConcurrentHashMap<String, String> myPrivateKeyMap = new ConcurrentHashMap<String, String>();

    public static ConcurrentHashMap<String, String> getMyPrivateKeyMapInstant() {
        return myPrivateKeyMap;
    }

    /**
     * 用map存放接收到好友的public Key，map的key为好友的id，value为好友的publicKey
     */
    private static final ConcurrentHashMap<String, String> friendPublicKeyMap = new ConcurrentHashMap<String, String>();

    public static ConcurrentHashMap<String, String> getFriendPublicKeyMapInstant() {
        return friendPublicKeyMap;
    }

    /**
     * 用map存放用于握手的随机数，map的key为好友的id，value为随机数
     */
    private static final ConcurrentHashMap<String, Integer> friendRandomMap = new ConcurrentHashMap<String, Integer>();

    public static ConcurrentHashMap<String, Integer> getfriendRandomMapInstant() {
        return friendRandomMap;
    }

    /**
     * 用Map存放对应好友的在线状态
     */
    private static final ConcurrentHashMap<String, String> onlineStatusRandomMap = new ConcurrentHashMap<String, String>();

    public static ConcurrentHashMap<String, String> getonlineStatusRandomMapInstant() {
        return onlineStatusRandomMap;
    }
}
