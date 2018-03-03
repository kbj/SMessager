package me.weey.graduationproject.client.smessager.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.vondear.rxtools.RxDataTool;
import com.vondear.rxtools.RxEncodeTool;
import com.vondear.rxtools.RxFileTool;
import com.vondear.rxtools.RxPhotoTool;
import com.vondear.rxtools.RxPictureTool;
import com.vondear.rxtools.RxTimeTool;
import com.vondear.rxtools.view.RxToast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.adapter.ChatAdapter;
import me.weey.graduationproject.client.smessager.entity.ChatMessage;
import me.weey.graduationproject.client.smessager.entity.Msg;
import me.weey.graduationproject.client.smessager.entity.User;
import me.weey.graduationproject.client.smessager.glide.GlideApp;
import me.weey.graduationproject.client.smessager.service.LoginHandlerService;
import me.weey.graduationproject.client.smessager.sqlite.ChatListOpenHelper;
import me.weey.graduationproject.client.smessager.utils.AESUtil;
import me.weey.graduationproject.client.smessager.utils.Constant;
import me.weey.graduationproject.client.smessager.utils.KeyBoardUtils;
import me.weey.graduationproject.client.smessager.utils.MPermissionUtils;
import me.weey.graduationproject.client.smessager.utils.MediaManager;
import me.weey.graduationproject.client.smessager.utils.SPUtil;
import me.weey.graduationproject.client.smessager.utils.UIUtil;
import me.weey.graduationproject.client.smessager.widget.AudioRecordButton;

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
    @BindView(R.id.voice_btn) AudioRecordButton mRecordButton;
    private User mFriendList;
    private LoginHandlerService mLoginHandlerService;
    private ChatHandler mChatHandler;
    private User mMyUser;
    private ProgressDialog mProgressDialog;
    private ChatListOpenHelper chatListOpenHelper;
    private ChatAdapter mChatAdapter;
    private ArrayList<ChatMessage> mChatMessages;

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
                mChatMessages = new ArrayList<>();
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
                        chatMessage.setVoiceSecond(cursor.getString(6));
                        mChatMessages.add(chatMessage);
                    }
                    cursor.close();
                }
                //关闭数据库
                readableDatabase.close();
                readableDatabase.close();
                chatListOpenHelper.close();

                //设置Adapter
                if (mChatAdapter == null) {
                    mChatAdapter = new ChatAdapter(mChatMessages, getApplicationContext(), mMyUser, mFriendList);
                }
                //设置气泡点击事件
                mChatAdapter.setBubbleClickListener(clickBubbleListener);
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
                        mRecyclerViewChatMessage.setAdapter(mChatAdapter);
                        //设置增加或删除条目的动画
                        mRecyclerViewChatMessage.setItemAnimator(new DefaultItemAnimator());
                    }
                });
            }
        }.start();

        //初始化录音
        mRecordButton.setAudioFinishRecorderListener(new AudioRecordButton.AudioFinishRecorderListener() {
            @Override
            public void onStart() {
                //开始录音，如果此时还有在播放音乐的话要把音乐暂停
                if (ChatAdapter.mIsPlayingVoice) {
                    pauseVoice();
                }
            }

            @Override
            public void onFinished(float seconds, String filePath) {
                //录音结束，把消息封装发送，然后显示到RecyclerView
                sendVoice(seconds, filePath);
            }
        });
    }

    /**
     * 点击气泡的事件
     */
    private ChatAdapter.onClickBubbleListener clickBubbleListener = new ChatAdapter.onClickBubbleListener() {
        @Override
        public void onClick(View view, int position) {
            //获取消息类型
            ChatMessage chatMessage = mChatMessages.get(position);
            if (!(chatMessage.getMessageType() == Constant.CHAT_MESSAGE_TYPE_VOICE_NEW
                    || chatMessage.getMessageType() == Constant.CHAT_MESSAGE_TYPE_VOICE_HAVE_LISTEN)) {
                return;
            }

            AnimationDrawable animationDrawable = null;
            if (chatMessage.getChatType() == Constant.CHAT_TYPE_RECEIVE) {
                //是收到的消息
                animationDrawable = (AnimationDrawable) view.findViewById(R.id.iv_chat_message_receive_voice).getBackground();
                if (chatMessage.getMessageType() == Constant.CHAT_MESSAGE_TYPE_VOICE_NEW) {
                    //新消息
                    chatMessage.setMessageType(Constant.CHAT_MESSAGE_TYPE_VOICE_HAVE_LISTEN);
                    //通知Adapter更新List
                    mChatAdapter.notifyItemChanged(position);
                    //启动线程，更新数据库
                    updateVoiceState(chatMessage);
                }
            } else if (chatMessage.getChatType() == Constant.CHAT_TYPE_SEND) {
                animationDrawable = (AnimationDrawable) view.findViewById(R.id.iv_chat_message_send_voice).getBackground();
            }
            //启动动画，播放语音
            if (animationDrawable == null) return;
            animationDrawable.start();
            ChatAdapter.mIsPlayingVoice = true;
            ChatAdapter.mPlayListPosition = position;
            MediaManager.playSound(chatMessage.getMessage(), new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    //播放完成需要停止动画
                    pauseVoice();
                }
            });
        }
    };

    /**
     * 更新语音的状态，改为已读的状态
     */
    private void updateVoiceState(final ChatMessage chatMessage) {
        new Thread() {
            @Override
            public void run() {
                if (chatListOpenHelper == null)
                    chatListOpenHelper = new ChatListOpenHelper(getApplicationContext(), Constant.CHAT_LIST_DB_NAME, null, 1);
                //新建一个数据库的连接
                SQLiteDatabase readableDatabase = chatListOpenHelper.getReadableDatabase();
                //开启事务
                readableDatabase.beginTransaction();
                //更新数据
                ContentValues contentValues = new ContentValues();
                contentValues.put("message_type", Constant.CHAT_MESSAGE_TYPE_VOICE_HAVE_LISTEN);
                readableDatabase.update("chat_message", contentValues, "id = ?", new String[]{chatMessage.getId()});
                //关闭事务
                readableDatabase.setTransactionSuccessful();
                readableDatabase.endTransaction();
                readableDatabase.close();
                chatListOpenHelper.close();
            }
        }.start();
    }

    /**
     * 暂停播放语音
     */
    private void pauseVoice() {
        if (ChatAdapter.mPlayListPosition != -1) {
            View view = mRecyclerViewChatMessage.getChildAt(ChatAdapter.mPlayListPosition);
            //判断是发送还是接收方
            View receive = view.findViewById(R.id.iv_chat_message_receive_voice);
            if (receive.getVisibility() == View.VISIBLE) {
                //是接受方
                AnimationDrawable drawable = (AnimationDrawable) receive.getBackground();
                drawable.stop();
                receive.setBackgroundResource(R.drawable.receive_voice_play_anim);
            } else {
                //是发送方
                View send = view.findViewById(R.id.iv_chat_message_send_voice);
                if (send != null) {
                    AnimationDrawable drawable = (AnimationDrawable) send.getBackground();
                    drawable.stop();
                    send.setBackgroundResource(R.drawable.send_voice_play_anim);
                }
            }
        }
        MediaManager.pause();
        ChatAdapter.mIsPlayingVoice = false;
        ChatAdapter.mPlayListPosition = -1;
    }

    /**
     * 录音结束后发送录音
     */
    private void sendVoice(final float seconds, final String filePath) {
        //获取当前时间
        final String time = RxTimeTool.date2String(new Date());
        new Thread() {
            @Override
            public void run() {
                //将语音文件转成字节数组
                if (!RxFileTool.isFileExists(filePath)) return;
                try {
                    //获取AES密码
                    String aesKey = Constant.getAesKeyMapInstant().get(mFriendList.getId());
                    FileInputStream inputStream = new FileInputStream(filePath);
                    byte[] voiceByte = RxDataTool.inputStream2Bytes(inputStream);
                    //加密数据
                    byte[] aes256Encode = AESUtil.Aes256Encode(voiceByte, RxEncodeTool.base64Decode(aesKey));
                    //使用Msg来包装内容
                    Msg msg = new Msg();
                    msg.setMsgType(Constant.CHAT_MESSAGE_TYPE_VOICE_HAVE_LISTEN);
                    msg.setMessage(aes256Encode);
                    //调用Service发送消息
                    mLoginHandlerService.sendMessage(mFriendList.getId(), JSON.toJSONString(msg),
                            Constant.MESSAGE_TYPE_SEND_MESSAGE, Constant.MODEL_TYPE_CHAT, 7);
                    //保存到数据库
                    mLoginHandlerService.saveMessageToDB(filePath,
                            msg.getMsgType(), time, mFriendList.getId(), true, ((int)seconds)+"");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        //封装ChatMessage新增到RecyclerView里面
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setMessageType(Constant.CHAT_MESSAGE_TYPE_VOICE_HAVE_LISTEN);
        chatMessage.setMessage(filePath);
        chatMessage.setChatType(Constant.CHAT_TYPE_SEND);
        chatMessage.setTime(time);
        if (seconds < 1) {
            //小于1秒的统一都设置成1
            chatMessage.setVoiceSecond("1");
        } else {
            //超过1秒的改为四舍五入整数
            chatMessage.setVoiceSecond(Math.round(seconds)+"");
        }
        chatMessage.setUserId(mFriendList.getId());
        //添加进Adapter
        mChatAdapter.addMessage(chatMessage);
    }

    /**
     * 语音和文字的切换按钮
     */
    @OnClick(R.id.iv_voice)
    public void initVoiceSwitcher(View view) {
        //录音按钮隐藏，也就是文本编辑模式的时候
        if (mRecordButton.getVisibility() == View.GONE) {
            //需要先申请权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                askRecordPermission(view);
            } else {
                //不需要运行时权限
                //点击以后隐藏文本输入框
                mChatInputMessage.setVisibility(View.GONE);
                //显示输入框
                mRecordButton.setVisibility(View.VISIBLE);
                //隐藏键盘输入
                KeyBoardUtils.hideKeyBoard(ChatActivity.this, mChatInputMessage);
                //设置更改按钮的背景图为切换回键盘
                view.setBackgroundResource(R.mipmap.keyboard_grey);
            }
        } else {
            //要切换回文本模式
            mChatInputMessage.setVisibility(View.VISIBLE);
            mRecordButton.setVisibility(View.GONE);
            view.setBackgroundResource(R.mipmap.keyboard_voice_grey);
            //弹出键盘
            KeyBoardUtils.showKeyBoard(ChatActivity.this, mChatInputMessage);
        }
    }

    /**
     * 点击发送图片的按钮
     */
    @OnClick(R.id.iv_insert_photo)
    public void sendImage(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"),5002);
    }

    /**
     * 启动其他Activity返回的数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 5002:
                //发送选取的图片
                if (data == null) return;
                //把data中的URI转为图片文件的URL地址
                String imageAbsolutePath = RxPhotoTool.getImageAbsolutePath(ChatActivity.this, data.getData());
                if (imageAbsolutePath == null || !new File(imageAbsolutePath).exists()) return;

                sendImageMessage(imageAbsolutePath);
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 根据传来的要发送的图片文本地址发送图片
     */
    private void sendImageMessage(String imageAbsolutePath) {
        //需要对原图压缩80%后存到media文件夹里面

    }

    /**
     * 申请运行时的录音权限
     */
    private void askRecordPermission(final View view) {
        MPermissionUtils.requestPermissionsResult(ChatActivity.this, Constant.PERMISSION_RECORD_AUDIO,
                new String[]{Manifest.permission.RECORD_AUDIO}, new MPermissionUtils.OnPermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        //授权同意
                        //点击以后隐藏文本输入框
                        mChatInputMessage.setVisibility(View.GONE);
                        //显示输入框
                        mRecordButton.setVisibility(View.VISIBLE);
                        //隐藏键盘输入
                        KeyBoardUtils.hideKeyBoard(ChatActivity.this, mChatInputMessage);
                        //设置更改按钮的背景图为切换回键盘
                        view.setBackgroundResource(R.mipmap.keyboard_grey);
                    }

                    @Override
                    public void onPermissionDenied() {
                        //当权限拒绝的时候
                        MPermissionUtils.showTipsDialog(ChatActivity.this);
                    }
                });
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
                try {
                    //加密
                    String aesKey = Constant.getAesKeyMapInstant().get(mFriendList.getId());
                    byte[] aes256Encode = AESUtil.Aes256Encode(inputMsg.getBytes("UTF-8"), RxEncodeTool.base64Decode(aesKey));
                    //发送信息
                    Msg msg = new Msg();
                    msg.setMessage(aes256Encode);
                    msg.setMsgType(Constant.CHAT_MESSAGE_TYPE_TEXT);
                    mLoginHandlerService.sendMessage(mFriendList.getId(), JSON.toJSONString(msg), Constant.MESSAGE_TYPE_SEND_MESSAGE, Constant.MODEL_TYPE_CHAT, 7);
                    //保存到数据库 todo:先暂时写成空
                    mLoginHandlerService.saveMessageToDB(inputMsg, Constant.CHAT_MESSAGE_TYPE_TEXT, time, mFriendList.getId(), true, "");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        //加入到RecyclerView中 todo
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setMessageType(Constant.CHAT_MESSAGE_TYPE_TEXT);
        chatMessage.setMessage(inputMsg);
        chatMessage.setChatType(Constant.CHAT_TYPE_SEND);
        chatMessage.setTime(time);
        chatMessage.setUserId(mFriendList.getId());

        mChatAdapter.addMessage(chatMessage);

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

    /**
     * 申请运行时权限结果的回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //用工具类的回调方法
        MPermissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

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
                    chatActivity.mChatAdapter.addMessage(chatMessage);
                    break;
            }
        }
    }

}
