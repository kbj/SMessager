package me.weey.graduationproject.client.smessager.activity;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.bumptech.glide.util.LogTime;
import com.vondear.rxtools.RxEncodeTool;
import com.vondear.rxtools.RxEncryptTool;
import com.vondear.rxtools.RxThreadPoolTool;
import com.vondear.rxtools.RxTimeTool;
import com.vondear.rxtools.view.RxToast;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
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
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.adapter.ChatAdapter;
import me.weey.graduationproject.client.smessager.adapter.ChatListAdapter;
import me.weey.graduationproject.client.smessager.entity.ChatMessage;
import me.weey.graduationproject.client.smessager.entity.DataStructure;
import me.weey.graduationproject.client.smessager.entity.Msg;
import me.weey.graduationproject.client.smessager.entity.User;
import me.weey.graduationproject.client.smessager.glide.GlideApp;
import me.weey.graduationproject.client.smessager.service.LoginHandlerService;
import me.weey.graduationproject.client.smessager.sqlite.ChatListOpenHelper;
import me.weey.graduationproject.client.smessager.utils.AESUtil;
import me.weey.graduationproject.client.smessager.utils.Constant;
import me.weey.graduationproject.client.smessager.utils.ECDHUtil;
import me.weey.graduationproject.client.smessager.utils.ECDSAUtil;
import me.weey.graduationproject.client.smessager.utils.KeyUtil;
import me.weey.graduationproject.client.smessager.utils.SPUtil;
import me.weey.graduationproject.client.smessager.utils.UIUtil;

/**
 * 具体聊天页面的Activity
 * Created by weikai on 2018/02/11/0011.
 */

public class ChatActivity extends AppCompatActivity {
    public static final String CHAT_INFO = "chat_info";
    private static final int BIND_SERVICE_SUCCESS = 5678;

    private final static Type type = new TypeReference<HashMap<String, String>>() {}.getType();
    //判断是否在前台
    public static boolean isFront = false;

    @BindView(R.id.tb_chat) Toolbar mToolBar;
    @BindView(R.id.tv_chat_user_name) TextView mChatUserName;
    @BindView(R.id.tv_last_message_time) TextView mChatLastMessageTime;
    @BindView(R.id.rv_chat_message) RecyclerView mRecyclerViewChatMessage;
    @BindView(R.id.et_input_message) EditText mChatInputMessage;
    @BindView(R.id.iv_send_message) ImageView mSendMessage;
    @BindView(R.id.iv_chat_avatar) ImageView mChatFriendsAvatar;
    private User mFriendList;
    private LoginHandlerService mLoginHandlerService;
    private ChatHandler mChatHandler;
    private User mMyUser;
    private ProgressDialog mProgressDialog;
    private ChatListOpenHelper chatListOpenHelper;
    private ChatAdapter mChatAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //获取主题的颜色
        int color = UIUtil.getThemeColorAccent(ChatActivity.this);
        //设置导航栏颜色
        getWindow().setNavigationBarColor(color);
        setContentView(R.layout.activity_chat);
        //绑定UI
        ButterKnife.bind(this);
        mChatHandler = new ChatHandler(this);
        //绑定服务
        initService();
        //标题栏显示用户相关内容
        initUser();
        //初始化UI
        initUI();
    }


    /**
     * 初始化UI
     */
    private void initUI() {
        //显示向后退的按钮
        mToolBar.setTitle("");
        setSupportActionBar(mToolBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);

            //后退按钮的点击事件
            mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                    finish();
                }
            });
        }
        //后退按钮设置成白色
        Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_material);
        if (upArrow == null) return;
        upArrow.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        //显示一个Dialog提示正在建立连接
        mProgressDialog = new ProgressDialog(ChatActivity.this);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setMessage("正在建立加密通信连接...");
        mProgressDialog.show();

        new Thread() {
            @Override
            public void run() {
                //从数据库取出聊天信息的记录
                final ArrayList<ChatMessage> chatMessages = new ArrayList<>();
                if (chatListOpenHelper == null)
                    chatListOpenHelper = new ChatListOpenHelper(getApplicationContext(), Constant.CHAT_LIST_DB_NAME, null, 1);
                //新建一个数据库的连接
                SQLiteDatabase readableDatabase = chatListOpenHelper.getReadableDatabase();
                //定义查询
                Cursor cursor = readableDatabase.query("chat_message", null, "user_id = ?", new String[]{mFriendList.getId()}, null, null, "time", null);
                if (cursor != null && cursor.getCount() > 0) {
                    //有记录
                    while (cursor.moveToNext()) {
                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.setId(cursor.getString(0));
                        chatMessage.setUserId(cursor.getString(1));
                        chatMessage.setMessage(cursor.getString(2));
                        chatMessage.setTime(cursor.getString(3));
                        chatMessage.setChatType(cursor.getInt(4));
                        chatMessage.setMessageType(cursor.getInt(5));
                        chatMessages.add(chatMessage);
                    }
                    cursor.close();
                }
                //关闭数据库
                readableDatabase.close();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //初始化RecycleView
                        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
                        //设置布局管理器
                        mRecyclerViewChatMessage.setLayoutManager(layoutManager);
                        //设置为垂直布局，这也是默认的
                        layoutManager.setOrientation(OrientationHelper.VERTICAL);
                        //设置Adapter
                        mChatAdapter = new ChatAdapter(chatMessages, getApplicationContext(), mMyUser, mFriendList);
                        mRecyclerViewChatMessage.setAdapter(mChatAdapter);
                        //设置增加或删除条目的动画
                        mRecyclerViewChatMessage.setItemAnimator(new DefaultItemAnimator());
                    }
                });
            }
        }.start();
    }

    /**
     * 显示聊天对象的信息
     */
    private void initUser() {
        //取出当前用户的信息
        if (mMyUser == null) {
            mMyUser = JSON.parseObject(SPUtil.getString(getApplicationContext(), Constant.USER_INFO), User.class);
        }
        //取出intent中的数据
        mFriendList = (User) getIntent().getSerializableExtra(CHAT_INFO);
        String avatarURL = Constant.SERVER_ADDRESS + "account/avatars/" + mFriendList.getId();
        if (mFriendList == null) return;
        //初始化加密流程的Map
        Constant.getProcessMapInstant().put(mFriendList.getId(), 0);
        //设置参数
        mChatUserName.setText(mFriendList.getUserName());
        //显示头像
        GlideApp.with(getApplicationContext())
                .load(avatarURL)
                //占位图
                .placeholder(R.mipmap.book_user)
                //圆形显示
                .circleCrop()
                //错误占位符
                .error(R.mipmap.book_user)
                //后备回调符
                .fallback(R.mipmap.book_user)
                .into(mChatFriendsAvatar);
    }

    /**
     * 初始化服务，连接
     */
    private void initService() {
        Intent intent = new Intent(ChatActivity.this, LoginHandlerService.class);
        intent.putExtra(LoginHandlerService.CHAT_ACTIVITY_HANDLER, new Messenger(mChatHandler));
        startService(intent);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    @OnClick(R.id.iv_send_message)
    public void sendMsg() {
        //获取要发送的内容
        final String inputMsg = mChatInputMessage.getText().toString().trim();
        final String time = RxTimeTool.date2String(new Date());
        if (TextUtils.isEmpty(inputMsg)) return;
        new Thread(){
            @Override
            public void run() {
                //加密
                String aesKey = Constant.getAesKeyMapInstant().get(mFriendList.getId());
                byte[] aes256Encode = AESUtil.Aes256Encode(inputMsg, RxEncodeTool.base64Decode(aesKey));
                //发送信息
                Msg msg = new Msg();
                msg.setMessage(aes256Encode);
                msg.setMsgType(0);
                mLoginHandlerService.sendMessage(mFriendList.getId(), JSON.toJSONString(msg), Constant.MESSAGE_TYPE_SEND_MESSAGE, Constant.MODEL_TYPE_CHAT, 7);
                //保存到数据库 todo:先暂时写成空
                mLoginHandlerService.saveMessageToDB(inputMsg, 0, time, "", mFriendList.getId(), true);
            }
        }.start();

        //加入到RecyclerView中
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setMessageType(0);
        chatMessage.setMessage(inputMsg);
        chatMessage.setChatType(0);
        chatMessage.setTime(time);
        chatMessage.setUserId(mFriendList.getId());

        mChatAdapter.addTextMessage(chatMessage);

        //文本框回空
        mChatInputMessage.setText("");
    }



    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LoginHandlerService.LoginBinder binder = (LoginHandlerService.LoginBinder) service;
            mLoginHandlerService = binder.getService();
            //通知主线程服务绑定成功
            mChatHandler.sendEmptyMessage(BIND_SERVICE_SUCCESS);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLoginHandlerService = null;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
        Constant.getProcessMapInstant().put(mFriendList.getId(), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isFront = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isFront = false;
    }

    /**
     * 创建子类用于主线程与子线程通信
     */
    private static class ChatHandler extends Handler {
        //使用弱引用
        WeakReference<ChatActivity> weakReference;

        ChatHandler(ChatActivity chatActivity) {
            this.weakReference = new WeakReference<ChatActivity>(chatActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            //使用弱引用之前需要先Get
            ChatActivity chatActivity = weakReference.get();
            if (chatActivity == null) return;
            //针对传来的状态码解析
            switch (msg.what) {
                case BIND_SERVICE_SUCCESS:
                    //绑定服务成功
                    //开始建立连接
                    chatActivity.mLoginHandlerService.startChatProcess(chatActivity.mFriendList);
                    break;
                case LoginHandlerService.ESTABLISH_CONNECTION_SUCCESS:
                    //建立连接成功
                    if (chatActivity.mProgressDialog != null) {
                        chatActivity.mProgressDialog.dismiss();
                    }
                    break;
                case Constant.CODE_PROCESS_FAILURE:
                    //建立连接失败
                    if (chatActivity.mProgressDialog != null) {
                        chatActivity.mProgressDialog.dismiss();
                    }
                    RxToast.error("建立连接失败！信息：" + msg.obj);
                    break;
                case LoginHandlerService.RECEIVE_NEW_MESSAGE:
                    //收到新的消息
                    ChatMessage chatMessage = (ChatMessage) msg.obj;
                    chatActivity.mChatAdapter.addTextMessage(chatMessage);
                    break;
            }
        }
    }

}
