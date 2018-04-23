package me.weey.graduationproject.client.smessager.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;


import com.alibaba.fastjson.JSON;
import com.vondear.rxtools.RxActivityTool;
import com.vondear.rxtools.RxEncryptTool;
import com.vondear.rxtools.RxRegTool;
import com.vondear.rxtools.RxDeviceTool;
import com.vondear.rxtools.view.RxToast;

import java.io.IOException;
import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.entity.HttpResponse;
import me.weey.graduationproject.client.smessager.entity.User;
import me.weey.graduationproject.client.smessager.utils.CommonUtil;
import me.weey.graduationproject.client.smessager.utils.Constant;
import me.weey.graduationproject.client.smessager.utils.MPermissionUtils;
import me.weey.graduationproject.client.smessager.utils.UIUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 注册页的Activity
 */
public class RegisterActivity extends AppCompatActivity {

    @BindView(R.id.et_username) EditText etTextUserName;
    @BindView(R.id.et_password) EditText etPassword;
    @BindView(R.id.et_repeatpassword) EditText etRepeatPassword;
    @BindView(R.id.et_email) EditText etEmail;
    @BindView(R.id.bt_register) Button registerButton;
    @BindView(R.id.fab) FloatingActionButton floatingActionButton;
    @BindView(R.id.cv_register) CardView cvRegister;


    private ProgressDialog mProgressDialog;
    private RegisterHandler mHandler;


    public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    //HTTP请求失败
    private static final int HTTP_FAILURE = 5000;
    //注册失败
    private static final int REGISTER_ERROR = 5001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //获取主题的颜色
        int color = UIUtil.getThemeColorAccent(RegisterActivity.this);
        //设置沉浸式
        UIUtil.setTranslucentStatus(getWindow(), color);
        UIUtil.setStatusBarFontDark(true, getWindow());

        setContentView(R.layout.activity_register);
        //绑定UI
        ButterKnife.bind(this);
        //初始化Handler
        mHandler = new RegisterHandler(this);
        //处理UI
        initUI();
    }

    /**
     * 对UI的处理
     */
    private void initUI() {
        //播放进入Activity的动画
        ShowEnterAnimation();
        //浮动按钮的点击事件
        floatingActionButton.setOnClickListener(v -> animateRevealClose());
        //注册按钮的点击事件
        registerButton.setOnClickListener(v -> {
            //数据校验
            boolean validation = validation();
            if (!validation) return;
            //显示等待Dialog
            mProgressDialog = new ProgressDialog(RegisterActivity.this);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMessage("请稍候...");
            mProgressDialog.show();
            //按钮设置成disable
            registerButton.setActivated(false);
            //数据校验通过，需要申请运行时权限，获取IMEI信息
            //判断系统是否大于6.0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                MPermissionUtils.requestPermissionsResult(RegisterActivity.this, Constant.PERMISSION_READ_PHONE_STATE, new String[]{Manifest.permission.READ_PHONE_STATE},
                        new MPermissionUtils.OnPermissionListener() {
                            @Override
                            public void onPermissionGranted() {
                                //当权限同意的时候
                                sendMsgToRegister();
                            }

                            @Override
                            public void onPermissionDenied() {
                                //当权限拒绝的时候
                                MPermissionUtils.showTipsDialog(RegisterActivity.this);
                            }
                        });
            } else {
                //小于6.0的系统就直接请求了
                // 通过HTTP请求发送给服务器注册
                sendMsgToRegister();
            }
        });
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
     * 封装好数据包，通过HTTP的POST请求提交到服务器，并且获得反馈然后显示
     */
    private void sendMsgToRegister() {
        //封装密码
        String encryptPass = RxEncryptTool.encryptSHA512ToString(etPassword.getText().toString().trim());
        //封装信息
        User user = new User();
        user.setUserName(etTextUserName.getText().toString().trim().toLowerCase());
        user.setPassword(encryptPass);
        user.setEmail(etEmail.getText().toString().trim());
        user.setRegisterIp(Constant.IP_ADDRESS);
        user.setIMEI(RxDeviceTool.getIMEI(RegisterActivity.this));
        user.setRegisterBrand(RxDeviceTool.getBuildBrand());
        user.setRegisterModel(RxDeviceTool.getBuildBrandModel());
        Log.i("json", JSON.toJSONString(user));
        //封装参数信息
        RequestBody requestBody = RequestBody.create(JSON_MEDIA_TYPE, JSON.toJSONString(user));
        //创建请求
        Request build = new Request.Builder()
                .url(Constant.SERVER_ADDRESS + "/account/register")
                .post(requestBody)
                .build();
        //异步请求
        CommonUtil.getHttpClient().newCall(build).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandler.sendEmptyMessage(HTTP_FAILURE);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    //响应不成功
                    mHandler.sendEmptyMessage(HTTP_FAILURE);
                    return;
                }
                //解析接收到的内容
                String resp = response.body().string().trim();
                if (resp.isEmpty()) {
                    //响应不成功
                    mHandler.sendEmptyMessage(HTTP_FAILURE);
                    return;
                }
                Log.i("HTTPRESP", resp);
                HttpResponse httpResponse = JSON.parseObject(resp, HttpResponse.class);
                if (httpResponse == null) {
                    //响应不成功
                    mHandler.sendEmptyMessage(HTTP_FAILURE);
                    return;
                }
                if (httpResponse.getStatusCode().equals(Constant.CODE_SUCCESS)) {
                    //注册成功
                    mHandler.sendEmptyMessage(Constant.CODE_SUCCESS);
                } else {
                    //注册失败，那就把返回的状态信息直接传给Handler
                    Message message = mHandler.obtainMessage(REGISTER_ERROR);
                    message.obj = httpResponse.getMessage();
                    mHandler.sendMessage(message);
                }
            }
        });
    }

    /**
     * 点注册按钮的时候校验信息填写有效性
     */
    private boolean validation() {
        //获取字段
        String username = etTextUserName.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();
        String repeatPassword = etRepeatPassword.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        //校验
        if (TextUtils.isEmpty(username)) {
            //用户名为空
            RxToast.warning(getResources().getString(R.string.empty_username));
            return false;
        }
        if (!RxRegTool.isEmail(email)) {
            //不是邮件地址
            RxToast.warning(getResources().getString(R.string.error_email_address));
            return false;
        }
        if (!repeatPassword.equals(password)) {
            //确认密码不一致
            RxToast.warning(getResources().getString(R.string.password_repeat_password_not_same));
            return false;
        }
        if (repeatPassword.length() < 6) {
            int length = password.length();
            System.out.println(length);
            //密码长度小于6位
            RxToast.warning(getResources().getString(R.string.password_length_too_short));
            return false;
        }
        return true;
    }

    /**
     * 关闭的动画定制
     */
    private void animateRevealClose() {
        Animator mAnimator = ViewAnimationUtils.createCircularReveal(
                cvRegister,cvRegister.getWidth()/2,0,
                cvRegister.getHeight(), floatingActionButton.getWidth() / 2);
        mAnimator.setDuration(500);
        mAnimator.setInterpolator(new AccelerateInterpolator());
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                cvRegister.setVisibility(View.INVISIBLE);
                super.onAnimationEnd(animation);
                //图案更换
                floatingActionButton.setImageResource(R.drawable.plus);
                //按back键
                RegisterActivity.super.onBackPressed();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
            }
        });
        mAnimator.start();
    }

    /**
     * 启动Activity的动画
     */
    private void ShowEnterAnimation() {
        Transition transition = TransitionInflater.from(this).inflateTransition(R.transition.login_register_fab_transition);
        //设置动画
        getWindow().setSharedElementEnterTransition(transition);
        //设置动画的事件
        transition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                //动画开始的时候就把register的cardView隐藏
                cvRegister.setVisibility(View.GONE);
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                //动画结束时移除事件监听
                transition.removeListener(this);
                animateRevealShow();
            }

            @Override
            public void onTransitionCancel(Transition transition) {

            }

            @Override
            public void onTransitionPause(Transition transition) {

            }

            @Override
            public void onTransitionResume(Transition transition) {

            }
        });
    }

    /**
     * CardView移除的揭示动画显示
     */
    public void animateRevealShow() {
        Animator mAnimator = ViewAnimationUtils.createCircularReveal(
                cvRegister, cvRegister.getWidth()/2,0,
                floatingActionButton.getWidth() / 2, floatingActionButton.getHeight());
        mAnimator.setDuration(500);
        mAnimator.setInterpolator(new AccelerateInterpolator());
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                cvRegister.setVisibility(View.VISIBLE);
                super.onAnimationStart(animation);
            }
        });
        mAnimator.start();
    }

    /**
     * 处理back键的事件
     */
    @Override
    public void onBackPressed() {
        animateRevealClose();
    }

    /**
     * 创建一个Handler用于主线程与子线程之间的通信
     */
    private static class RegisterHandler extends Handler {
        //使用弱引用
        WeakReference<RegisterActivity> weakReference;

        RegisterHandler(RegisterActivity activity) {
            this.weakReference = new WeakReference<RegisterActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            //使用弱引用之前需要先Get
            RegisterActivity registerActivity = weakReference.get();
            if (registerActivity == null) return;
            //Dialog取消显示
            if (registerActivity.mProgressDialog.isShowing()) {
                registerActivity.mProgressDialog.dismiss();
            }
            //让按钮的状态恢复
            registerActivity.registerButton.setActivated(true);
            //根据传来的状态值执行不同的方法
            switch (msg.what) {
                case Constant.CODE_SUCCESS:
                    //注册成功，先提示用户去邮箱激活，然后跳转到登录页面
                    RxToast.success(registerActivity.getResources().getString(R.string.register_success));
                    //跳转
                    RxActivityTool.skipActivityAndFinish(registerActivity, LoginActivity.class);
                    break;
                case HTTP_FAILURE:
                    //Http请求失败
                    RxToast.error(registerActivity.getResources().getString(R.string.http_failure));
                    break;
                case REGISTER_ERROR:
                    //注册失败，获取服务器传来的错误信息
                    String errorMsg = (String) msg.obj;
                    RxToast.error(registerActivity.getResources().getString(R.string.register_failure) + errorMsg);
                    break;
            }
        }
    }
}
