package me.weey.graduationproject.client.smessager.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.vondear.rxtools.RxActivityTool;
import com.vondear.rxtools.RxDeviceTool;
import com.vondear.rxtools.RxEncryptTool;
import com.vondear.rxtools.RxNetTool;
import com.vondear.rxtools.RxTimeTool;
import com.vondear.rxtools.view.RxToast;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.adapter.ChatListAdapter;
import me.weey.graduationproject.client.smessager.entity.ChatList;
import me.weey.graduationproject.client.smessager.entity.DataStructure;
import me.weey.graduationproject.client.smessager.entity.User;
import me.weey.graduationproject.client.smessager.service.LoginHandlerService;
import me.weey.graduationproject.client.smessager.sqlite.ChatListOpenHelper;
import me.weey.graduationproject.client.smessager.utils.Constant;
import me.weey.graduationproject.client.smessager.utils.SPUtil;
import me.weey.graduationproject.client.smessager.utils.UIUtil;

/**
 * 好友列表的Activity
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String CHAT_MESSAGE_TABLE_NAME = "chat_message";
    public static final String LOGOUT_BROADCAST = "MainActivity.BindService.Broadcast";

    private static final String TAG = "MainActivity";

    @BindView(R.id.ll_main_content) RelativeLayout mMainContent;
    @BindView(R.id.tb) Toolbar mToolbar;
    @BindView(R.id.nav) NavigationView mNavigation;
    @BindView(R.id.activity_na) DrawerLayout mDrawerLayout;
    @BindView(R.id.rv_chat) RecyclerView mRecyclerChat;

    private MainHandler mMainHandler;
    private LoginHandlerService mLoginHandlerService;
    private User user;
    public final static Type type = new TypeReference<List<User>>() {}.getType();
    private ChatListAdapter mChatListAdapter;
    private LogoutBroadcast mLogoutBroadcast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //获取主题的颜色
        int color = UIUtil.getThemeColorAccent(MainActivity.this);
        //设置沉浸式
        UIUtil.setTranslucentStatus(getWindow(), color);
        setContentView(R.layout.activity_main);
        //初始化Handler
        mMainHandler = new MainHandler(this);
        //绑定UI
        ButterKnife.bind(this);
        //读取SP中的信息进行登录
        login();
        //取出数据库存的聊天列表
        ArrayList<ChatList> chatLists = initChatList();
        //判断是否需要下载头像缓存
        boolean isCache = cacheAvatar(chatLists);
        //初始化UI
        initUI(chatLists, isCache);
        //初始化广播
        mLogoutBroadcast = new LogoutBroadcast();
        IntentFilter intentFilter = new IntentFilter(LOGOUT_BROADCAST);
        registerReceiver(mLogoutBroadcast, intentFilter);
    }

    /**
     * WebSocket登录
     */
    private void login() {
        if (Constant.isOnLine) {
            mToolbar.setTitle(R.string.app_name);
        }
        //从SP中读取用户信息
        String userJSON = SPUtil.getString(getApplicationContext(), Constant.USER_INFO);
        //判断然后转换
        try {
            user = JSON.parseObject(userJSON, User.class);
        } catch (Exception e) {
            Log.i("MainActivity", e.getMessage());
            //说明保存的有问题，就直接跳转到登录页
            RxToast.error(getResources().getString(R.string.user_info_error));
            RxActivityTool.skipActivityAndFinish(getApplicationContext(), LoginActivity.class);
            return;
        }
        //判断登录信息是否完整
        if (TextUtils.isEmpty(user.getUserName()) || TextUtils.isEmpty(user.getPassword())) {
            //说明保存的有问题，就直接跳转到登录页
            RxToast.error(getResources().getString(R.string.user_info_error));
            RxActivityTool.skipActivityAndFinish(getApplicationContext(), LoginActivity.class);
            return;
        }
        //密码混淆
        String encryptPwd = RxEncryptTool.encryptSHA512ToString(user.getPassword());
        user.setPassword(encryptPwd);
        //开启服务完成登录
        Intent intent = new Intent(MainActivity.this, LoginHandlerService.class);
        //把Activity的Handler传入Service
        intent.putExtra(LoginHandlerService.LOGIN_ACTIVITY_HANDLER, new Messenger(mMainHandler));
        startService(intent);
        //开启绑定服务
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    /**
     * 判断头像是否已经缓存到本地，如果没有缓存，然后又是在WiFi环境下缓存头像
     */
    private boolean cacheAvatar(ArrayList<ChatList> chatLists) {
        //读取sp中的是否需要在非WIFI环境下缓存加载图片的设置
        Boolean isLoad = SPUtil.getBoolean(getApplicationContext(), Constant.LOAD_PICTURE_WITHOUT_WIFI, false);
        //获取当前是wifi还是移动网络
        boolean networkAvailable = RxNetTool.isNetworkAvailable(getApplicationContext());
        Log.i("MainActivity", "网络的情况为：" + networkAvailable);
        //网络不可用的情况下就不加载了
        if (!networkAvailable) return false;
        boolean isWifi = RxNetTool.isWifi(getApplicationContext());
        Log.i("MainActivity", "WIFI的情况为：" + isWifi);
        if (!isLoad) {
            //只有在WIFI的情况下才需要加载
            if (!isWifi) return false;
        }
        //不论啥情况都加载
        return true;
    }

    /**
     * 取出数据库存的聊天列表
     */
    private ArrayList<ChatList> initChatList() {
        ArrayList<ChatList> chatLists = new ArrayList<>();
        ChatListOpenHelper chatListOpenHelper = new ChatListOpenHelper(getApplicationContext(), Constant.CHAT_LIST_DB_NAME, null, 1);
        //新建一个数据库的连接
        SQLiteDatabase readableDatabase = chatListOpenHelper.getReadableDatabase();
        /*
         *  table：表名称
         *  colums：列名称数组，如果是查询所有可以写null
         *  selection：条件子句，相当于where
         *  selectionArgs：条件语句的参数数组
         *  groupBy：分组
         *  having：分组条件
         *  orderBy：排序类
         *  limit：分页查询的限制
         *  Cursor：返回值，相当于结果集ResultSet
         */
        //查询数据库
        Cursor cursor = readableDatabase.query(CHAT_MESSAGE_TABLE_NAME,
                new String[]{"user_id", "message", "time", "message_type", "is_new_message"},
                "myId = ?", new String[]{user.getId()}, "user_id", null, "DATETIME(time) desc", null);
        if (cursor != null && cursor.getCount() > 0) {
            //有记录
            while (cursor.moveToNext()) {
                ChatList chatList = new ChatList();
                //取出结果封装
                chatList.setUserId(cursor.getString(0).trim());
                chatList.setMessage(cursor.getString(1).trim());
                String timeFormat = cursor.getString(2);
                chatList.setTime(RxTimeTool.string2Date("yyyy-MM-dd HH:mm:ss", timeFormat));
                chatList.setMessageType(cursor.getInt(3));
                int isNew = cursor.getInt(4);
                if (isNew == 1) {
                    chatList.setNewMessage(true);
                } else {
                    chatList.setNewMessage(false);
                }
                chatLists.add(chatList);
            }
            cursor.close();
        }

        //关闭数据库
        readableDatabase.close();
        return chatLists;
    }

    /**
     * 初始化UI
     */
    private void initUI(ArrayList<ChatList> chatLists, Boolean isCache) {
        //给ToolBar设置侧滑得三条横线
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        //设置左上角显示三道横线
        toggle.syncState();
        //初始化RecycleView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        //设置布局管理器
        mRecyclerChat.setLayoutManager(layoutManager);
        //设置为垂直布局，这也是默认的
        layoutManager.setOrientation(OrientationHelper.VERTICAL);
        //设置Adapter
        mChatListAdapter = new ChatListAdapter(chatLists, this, isCache);
        mRecyclerChat.setAdapter(mChatListAdapter);
        //设置分隔线
        //mRecyclerChat.addItemDecoration( new DividerGridItemDecoration(this));
        //设置增加或删除条目的动画
        mRecyclerChat.setItemAnimator(new DefaultItemAnimator());
        //设置侧滑栏的的背景图
        View headerView = mNavigation.getHeaderView(0);
        TextView userName = headerView.findViewById(R.id.tv_userName);
        TextView email = headerView.findViewById(R.id.tv_email_address);
        userName.setText(user.getUserName());
        email.setText(user.getEmail());
        /*
         * 设置Adapter的点击事件
         */
        mChatListAdapter.setOnItemClickListener(new ChatListAdapter.onItemClickListener() {
            @Override
            public void onItemClick(String userID) {
                //获取对应好友的信息
                for (User friend : Constant.getFriendsListInstant()) {
                    if (friend.getId().equals(userID)) {
                        //设置好Intent传递信息
                        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                        intent.putExtra(ChatActivity.CHAT_INFO, friend);
                        startActivity(intent);
                        break;
                    }
                }
            }
        });
        //条目的点击事件
        mNavigation.setNavigationItemSelectedListener(this);
    }

    /**
     * 右下角新建聊天的浮动按钮的点击事件
     */
    @OnClick(R.id.fb_start_chatting)
    public void floatButtonClick() {
        RxActivityTool.skipActivity(this, NewChatListActivity.class);
    }

    /**
     * 侧滑栏条目的选择事件
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        //需要判断点击
        switch (item.getItemId()) {
            case R.id.nav_contacts:
                //点击到联系人
                RxActivityTool.skipActivity(MainActivity.this, NewChatListActivity.class);
                break;
            case R.id.nav_setting:
                //点击设置
                RxActivityTool.skipActivity(MainActivity.this, SettingActivity.class);
                break;
        }
        //导航栏回收
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return false;
    }

    /**
     * 把用户信息封装成DataStructure类型
     */
    private String initSendData(User user) {
        if (user == null) return JSON.toJSONString("");
        user.setRegisterIp(Constant.IP_ADDRESS);
        user.setIMEI(RxDeviceTool.getIMEI(MainActivity.this));
        user.setRegisterBrand(RxDeviceTool.getBuildBrand());
        user.setRegisterModel(RxDeviceTool.getBuildBrandModel());
        //将user转为JSON后作为DataStructure的内容参数
        DataStructure dataStructure = new DataStructure();
        dataStructure.setTime(new Date());
        dataStructure.setFromId("");
        dataStructure.setMessage(JSON.toJSONString(user));
        dataStructure.setMessageType(Constant.MESSAGE_TYPE_LOGIN);
        dataStructure.setModelType(Constant.MODEL_TYPE_ACCOUNT);
        dataStructure.setProcess(0);
        dataStructure.setToID(Constant.SERVER_ID);
        return JSON.toJSONString(dataStructure);
    }

    /**
     * 登录成功后获取好友列表
     */
    private void getFriendsList() {
        //请求
        mLoginHandlerService.sendMessage(Constant.SERVER_ID, "", -1,
                Constant.MESSAGE_TYPE_GET_FRIENDS_LIST, Constant.MODEL_TYPE_ACCOUNT, 0, new Date());
    }


    @Override
    protected void onDestroy() {
        //解除服务绑定
        this.unbindService(mConnection);
        //mLoginHandlerService.stopSelf();
        //停止绑定广播
        unregisterReceiver(mLogoutBroadcast);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //要更新recyclerView
        if (mRecyclerChat == null || mChatListAdapter == null) {
            return;
        }
        ArrayList<ChatList> chatLists = initChatList();
        mChatListAdapter.updateRecord(chatLists);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LoginHandlerService.LoginBinder binder = (LoginHandlerService.LoginBinder) service;
            mLoginHandlerService = binder.getService();

            //调用登录的方法
            mLoginHandlerService.socketLogin(initSendData(user));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLoginHandlerService = null;
        }
    };


    /**
     * 创建子类用于主线程与子线程通信
     */
    private static class MainHandler extends Handler {

        //使用弱引用
        WeakReference<MainActivity> weakReference;

        MainHandler(MainActivity mainActivity) {
            this.weakReference = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            //使用弱引用之前需要先Get
            MainActivity mainActivity = weakReference.get();
            if (mainActivity == null) return;
            //针对传来的状态码解析
            switch (msg.what) {
                case Constant.CODE_SUCCESS:
                    //表示登录成功了，先暂时把首页的Connecting换成SMessager
                    mainActivity.mToolbar.setTitle(R.string.app_name);
                    //发送给服务器请求好友列表
                    mainActivity.getFriendsList();
                    break;
                case Constant.CODE_CONNECTION_LOST:
                    //连接断开了 todo:要尝试重连
                    if (!Constant.isOnLine) {
                        mainActivity.mToolbar.setTitle(R.string.waiting_for_network);
                    }
                    break;
                case LoginHandlerService.GET_FRIENDS_LIST:
                    //请求获取好友列表的回应
                    RxToast.info(String.valueOf(msg.obj));
                    break;
                default:
                    //其他情况
                    mainActivity.mToolbar.setTitle(R.string.waiting_for_network);
                    RxToast.error(mainActivity.getResources().getString(R.string.http_failure));
                    SPUtil.saveString(mainActivity, Constant.USER_INFO, "");
                    RxActivityTool.skipActivityAndFinish(mainActivity, LoginActivity.class);
                    break;
            }
        }
    }

    /**
     * 注销登录的广播事件
     */
    public class LogoutBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mLoginHandlerService.logout();
            RxActivityTool.skipActivityAndFinish(MainActivity.this, LoginActivity.class);
        }
    }

}
