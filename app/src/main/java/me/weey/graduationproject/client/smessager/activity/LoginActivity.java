package me.weey.graduationproject.client.smessager.activity;


import android.Manifest;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.transition.Explode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.alibaba.fastjson.JSON;
import com.vondear.rxtools.RxActivityTool;
import com.vondear.rxtools.RxDeviceTool;
import com.vondear.rxtools.view.RxToast;

import java.lang.ref.WeakReference;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.entity.DataStructure;
import me.weey.graduationproject.client.smessager.entity.User;
import me.weey.graduationproject.client.smessager.service.LoginHandlerService;
import me.weey.graduationproject.client.smessager.utils.Constant;
import me.weey.graduationproject.client.smessager.utils.MPermissionUtils;
import me.weey.graduationproject.client.smessager.utils.SPUtil;
import me.weey.graduationproject.client.smessager.utils.UIUtil;

/**
 * 登录的Activity
 * Created by weikai on 2018/01/28/0028.
 */
public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.et_username) EditText userName;
    @BindView(R.id.et_password) EditText password;
    @BindView(R.id.bt_login) Button btLogin;
    @BindView(R.id.cv_login) CardView cvLogin;
    @BindView(R.id.fab) FloatingActionButton floatingActionButton;
    private LoginHandler mLoginHandler;
    private String userInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //获取主题的颜色
        int color = UIUtil.getThemeColorAccent(LoginActivity.this);
        //设置沉浸式
        UIUtil.setTranslucentStatus(getWindow(), color);
        UIUtil.setStatusBarFontDark(true, getWindow());

        setContentView(R.layout.activity_login);
        //绑定UI
        ButterKnife.bind(this);
        //初始化Handler
        mLoginHandler = new LoginHandler(this);
    }


    /**
     * 登录按钮的点击事件
     */
    @OnClick(R.id.bt_login)
    public void clickLogin(final Button btLogin) {
        //点击后需要先校验输入的值，然后封装数据包，建立Socket通信发送给服务器
        final String name = userName.getText().toString().trim();
        final String pwd = password.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            RxToast.warning(getResources().getString(R.string.empty_username));
            return;
        }
        if (pwd.length() < 6) {
            RxToast.warning(getResources().getString(R.string.password_length_too_short));
            return;
        }
        //禁用按钮
        btLogin.setEnabled(false);
        //申请权限
        //数据校验通过，需要申请运行时权限，获取IMEI信息
        //判断系统是否大于6.0
        final String[] dataStructure = new String[1];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            MPermissionUtils.requestPermissionsResult(LoginActivity.this,
                    Constant.PERMISSION_READ_PHONE_STATE, new String[]{Manifest.permission.READ_PHONE_STATE},
                    new MPermissionUtils.OnPermissionListener() {
                        @Override
                        public void onPermissionGranted() {
                            //当权限同意的时候
                            //封装数据
                            dataStructure[0] = initSendData(name, pwd);
                            //调用登录方法，把封装好的信息发送
                            userInfo = dataStructure[0];
                            startServiceLogin();
                        }

                        @Override
                        public void onPermissionDenied() {
                            //当权限拒绝的时候
                            MPermissionUtils.showTipsDialog(LoginActivity.this);
                        }
                    });
        } else {
            //小于6.0的系统就直接请求了
            // 封装信息
            dataStructure[0] = initSendData(name, pwd);
            //调用登录方法，把封装好的信息发送
            userInfo = dataStructure[0];
            startServiceLogin();
        }
    }

    /**
     * 浮动按钮的点击事件
     */
    @OnClick(R.id.fab)
    public void clickFloatingButton() {
        getWindow().setExitTransition(null);
        getWindow().setEnterTransition(null);
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                LoginActivity.this, floatingActionButton, floatingActionButton.getTransitionName());
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class), options.toBundle());
    }

    /**
     * 申请运行时权限结果的回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //用工具类的回调方法
        MPermissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 把用户信息封装成DataStructure类型
     * @param name 用户名
     * @param pwd 密码
     */
    private String initSendData(String name, String pwd) {
        //先把用户名和密码封装成User对象
        User user = new User();
        user.setUserName(name);
        user.setPassword(pwd);
        user.setRegisterIp(Constant.IP_ADDRESS);
        user.setIMEI(RxDeviceTool.getIMEI(LoginActivity.this));
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
     * 使用WebSocket实现通信登录
     */
    private void startServiceLogin() {
        //启动一个混合型的Service来完成Socket网络的通信
        Intent intent = new Intent(LoginActivity.this, LoginHandlerService.class);
        //把Activity的Handler传入Service
        intent.putExtra(LoginHandlerService.LOGIN_ACTIVITY_HANDLER, new Messenger(mLoginHandler));
        startService(intent);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LoginHandlerService.LoginBinder binder = (LoginHandlerService.LoginBinder) service;
            LoginHandlerService mLoginHandlerService = binder.getService();

            //调用登录的方法
            mLoginHandlerService.socketLogin(userInfo);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onRestart() {
        super.onRestart();
        //重新开始的时候设置为空
        floatingActionButton.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //设置为可见
        floatingActionButton.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

    /**
     * 创建子类用于主线程与子线程通信
     */
    private static class LoginHandler extends Handler{
        //使用弱引用
        WeakReference<LoginActivity> weakReference;

        LoginHandler(LoginActivity loginActivity) {
            this.weakReference = new WeakReference<LoginActivity>(loginActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            //使用弱引用之前需要先Get
            LoginActivity loginActivity = weakReference.get();
            if (loginActivity == null) return;
            //解禁按钮
            loginActivity.btLogin.setEnabled(true);
            //针对传来的状态码解析
            switch (msg.what) {
                case Constant.CODE_SUCCESS:
                    //表示登录成功了
                    RxToast.success(loginActivity.getResources().getString(R.string.login_success));
                    // 保存用户名密码到sp，然后跳转
                    String from = (String) msg.obj;
                    User fromUser = JSON.parseObject(from, User.class);
                    fromUser.setPassword(loginActivity.password.getText().toString().trim());
                    SPUtil.saveString(loginActivity.getApplicationContext(),
                            Constant.USER_INFO, JSON.toJSONString(fromUser));
                    SPUtil.saveBoolean(loginActivity, Constant.FIRST_BOOT, false);
                    //设置点击事件的动画
                    Explode explode = new Explode();
                    explode.setDuration(500);
                    loginActivity.getWindow().setExitTransition(explode);
                    loginActivity.getWindow().setEnterTransition(explode);
                    ActivityOptionsCompat oc2 = ActivityOptionsCompat.makeSceneTransitionAnimation(loginActivity);
                    RxActivityTool.skipActivityAndFinish(loginActivity, MainActivity.class, oc2.toBundle());
                    break;
                case Constant.CODE_CHECK_FAILURE:
                    //校验失败
                    RxToast.error("校验失败！错误信息：" + msg.obj);
                    break;
                case Constant.CODE_FAILURE:
                    //登录失败
                    RxToast.error("登录失败！错误信息：" + msg.obj);
                    break;
            }
        }
    }
}
