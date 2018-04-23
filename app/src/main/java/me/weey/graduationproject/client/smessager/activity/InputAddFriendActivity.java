package me.weey.graduationproject.client.smessager.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.github.irvingryan.VerifyCodeView;
import com.vondear.rxtools.RxActivityTool;
import com.vondear.rxtools.view.RxToast;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.entity.HttpResponse;
import me.weey.graduationproject.client.smessager.entity.User;
import me.weey.graduationproject.client.smessager.utils.CommonUtil;
import me.weey.graduationproject.client.smessager.utils.Constant;
import me.weey.graduationproject.client.smessager.utils.KeyBoardUtils;
import me.weey.graduationproject.client.smessager.utils.SPUtil;
import me.weey.graduationproject.client.smessager.utils.UIUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 输入四位数随机码添加好友
 * Created by weikai on 2018/03/13/0013.
 */

public class InputAddFriendActivity extends AppCompatActivity {

    private static final String TAG = "InputAddFriendActivity";
    private static final int GET_CODE_FAILURE = 500;
    private static final int GET_CODE_SUCCESS = 200;
    private static final int SUBMIT_CODE_FAILURE = 501;
    private static final int SUBMIT_CODE_SUCCESS = 201;

    @BindView(R.id.et_code_input) VerifyCodeView mVerifyCodeView;
    @BindView(R.id.tv_code_hint) TextView mCodeHint;
    @BindView(R.id.waiting_dialog) AVLoadingIndicatorView mWaitingDialog;
    private CodeHandler mCodeHandler;
    private User mUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //获取主题的颜色
        int color = UIUtil.getThemeColorAccent(InputAddFriendActivity.this);
        //设置导航栏颜色
        getWindow().setNavigationBarColor(color);
        setContentView(R.layout.activity_add_friend);
        //绑定UI
        ButterKnife.bind(this);
        //显示键盘
        KeyBoardUtils.showKeyBoard(InputAddFriendActivity.this, mVerifyCodeView);
        //初始化Handler
        mCodeHandler = new CodeHandler(this);

        initUI();
    }

    /**
     * 初始化UI
     */
    private void initUI() {
        //为输入框设置改变事件
        mVerifyCodeView.setListener(new VerifyCodeView.OnTextChangListener() {
            @Override
            public void afterTextChanged(String text) {
                if (text.trim().length() == 4) {
                    //获取本机的用户ID
                    if (mUser == null) {
                        String userJSON = SPUtil.getString(getApplicationContext(), Constant.USER_INFO);
                        mUser = JSON.parseObject(userJSON, User.class);
                    }
                    Log.i(TAG, "afterTextChanged: 验证码提交给服务器：" + text);
                    //显示等待框
                    mWaitingDialog.setVisibility(View.VISIBLE);
                    mWaitingDialog.show();
                    //请求服务器
                    FormBody build = new FormBody.Builder()
                            .add("id", mUser.getId())
                            .add("randomCode", text.trim())
                            .build();
                    Request request = new Request.Builder()
                            .url(Constant.SERVER_ADDRESS + "account/add/friend")
                            .post(build)
                            .build();
                    CommonUtil.getHttpClient().newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Message obtain = Message.obtain();
                            obtain.what = SUBMIT_CODE_FAILURE;
                            obtain.obj = "网络请求失败！";
                            mCodeHandler.sendMessage(obtain);
                            Log.e(TAG, "onFailure: 网络请求失败！", e);
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (!response.isSuccessful()) {
                                Message obtain = Message.obtain();
                                obtain.what = SUBMIT_CODE_FAILURE;
                                obtain.obj = "添加失败！请重试！";
                                mCodeHandler.sendMessage(obtain);
                                return;
                            }
                            //获取传递来的文本信息
                            String resp = response.body().string();
                            Log.i(TAG, "onResponse: 接收传递来的信息：" + resp);
                            //JSON解析
                            HttpResponse httpResponse = JSON.parseObject(resp, HttpResponse.class);
                            switch (httpResponse.getStatusCode()) {
                                case Constant.CODE_FAILURE:
                                    //添加失败了
                                    Message obtain = Message.obtain();
                                    obtain.what = SUBMIT_CODE_FAILURE;
                                    obtain.obj = httpResponse.getMessage();
                                    mCodeHandler.sendMessage(obtain);
                                    break;
                                case Constant.CODE_SUCCESS:
                                    //添加好友成功
                                    Message obtain1 = Message.obtain();
                                    obtain1.what = SUBMIT_CODE_SUCCESS;
                                    obtain1.obj = httpResponse.getMessage();
                                    mCodeHandler.sendMessage(obtain1);
                                    break;
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 点击按钮，如果当前是输入状态就变成生成状态，否则相反
     */
    @OnClick(R.id.bt_change_generate_input)
    public void clickGenerateCode(Button button) {
        //获取数字输入框的状态
        if (mVerifyCodeView.isEnabled()) {
            //是输入状态，请求服务器获得随机码
            mVerifyCodeView.setText("");
            getRandomCode();
            //改变输入状态
            mVerifyCodeView.setEnabled(false);
            mVerifyCodeView.setActivated(false);
            mVerifyCodeView.setFocusable(false);
            //隐藏键盘
            KeyBoardUtils.hideKeyBoard(InputAddFriendActivity.this, mVerifyCodeView);
            //修改按钮的提示文本
            button.setText(R.string.input_friends_code);
            //修改数字栏上面的文本
            mCodeHint.setText(R.string.generate_code_input);
        } else {
            //当前是生成状态，点击后变成输入状态
            mVerifyCodeView.setText("");
            mVerifyCodeView.setEnabled(true);
            mVerifyCodeView.setActivated(true);
            mVerifyCodeView.setFocusable(true);
            //显示键盘
            KeyBoardUtils.showKeyBoard(InputAddFriendActivity.this, mVerifyCodeView);
            //修改按钮的提示
            button.setText(R.string.generate_my_code);
            //修改文本提示
            mCodeHint.setText(R.string.input_code_hint);
        }
    }

    /**
     * 获取到随机码以后显示到控件上面
     */
    private void showRandomCode(String message) {
        //显示到首页
        mVerifyCodeView.setText(message.trim());
        //将验证码添加到剪切板
        //获取剪贴板管理器：
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        // 创建普通字符型ClipData
        ClipData mClipData = ClipData.newPlainText(TAG, message.trim());
        // 将ClipData内容放到系统剪贴板里。
        cm.setPrimaryClip(mClipData);
    }

    /**
     * 请求服务器获得随机码
     */
    private void getRandomCode() {
        try {
            //获取本机的用户ID
            if (mUser == null) {
                String userJSON = SPUtil.getString(getApplicationContext(), Constant.USER_INFO);
                mUser = JSON.parseObject(userJSON, User.class);
            }
            String url = Constant.SERVER_ADDRESS + "/account/add/getRandomCode";
            FormBody formBody = new FormBody.Builder()
                    .add("id", mUser.getId())
                    .build();
            //创建请求
            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();
            CommonUtil.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Message obtain = Message.obtain();
                    obtain.what = GET_CODE_FAILURE;
                    obtain.obj = "网络连通有问题！";
                    mCodeHandler.sendMessage(obtain);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Message obtain = Message.obtain();
                        obtain.what = GET_CODE_FAILURE;
                        obtain.obj = "网络请求失败!";
                        mCodeHandler.sendMessage(obtain);
                        return;
                    }
                    //获取传来的值
                    String resp = response.body().string();
                    //转换
                    String num = JSON.parseObject(resp, String.class);
                    if (TextUtils.isEmpty(num)) {
                        Message obtain = Message.obtain();
                        obtain.what = GET_CODE_FAILURE;
                        obtain.obj = "获取随机码失败！";
                        mCodeHandler.sendMessage(obtain);
                        return;
                    }
                    //获取成功
                    Message obtain = Message.obtain();
                    obtain.what = GET_CODE_SUCCESS;
                    obtain.obj = num;
                    mCodeHandler.sendMessage(obtain);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "getRandomCode: ", e);
        }
    }

    private static class CodeHandler extends Handler {
        //使用弱引用
        WeakReference<InputAddFriendActivity> weakReference;
        CodeHandler(InputAddFriendActivity inputAddFriendActivity) {
            this.weakReference = new WeakReference<InputAddFriendActivity>(inputAddFriendActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            //需要先获取
            InputAddFriendActivity inputAddFriendActivity = weakReference.get();
            if (inputAddFriendActivity == null) return;
            String message = String.valueOf(msg.obj);
            switch (msg.what) {
                case GET_CODE_FAILURE:
                    //获取内容失败了
                    inputAddFriendActivity.mWaitingDialog.hide();
                    RxToast.error(message);
                    break;
                case GET_CODE_SUCCESS:
                    //获取随机码成功
                    inputAddFriendActivity.showRandomCode(message);
                    break;
                case SUBMIT_CODE_FAILURE:
                    //提交内容失败
                    inputAddFriendActivity.mWaitingDialog.hide();
                    inputAddFriendActivity.mVerifyCodeView.setText("");
                    RxToast.error(message);
                    break;
                case SUBMIT_CODE_SUCCESS:
                    //添加好友成功
                    inputAddFriendActivity.mWaitingDialog.hide();
                    //获取传来的好友信息更新
                    List<User> userList = JSON.parseObject(message, MainActivity.type);
                    //重新请求更新当前的好友列表
                    ArrayList<User> friendsListInstant = Constant.getFriendsListInstant();
                    friendsListInstant.clear();
                    friendsListInstant.addAll(userList);
                    RxToast.success("新增好友成功！");
                    inputAddFriendActivity.mVerifyCodeView.setText("");
                    inputAddFriendActivity.onBackPressed();
                    break;
            }
        }
    }
}
