package me.weey.graduationproject.client.smessager.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.vondear.rxtools.RxActivityTool;
import com.vondear.rxtools.RxEncryptTool;
import com.vondear.rxtools.view.RxToast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.entity.HttpResponse;
import me.weey.graduationproject.client.smessager.entity.User;
import me.weey.graduationproject.client.smessager.glide.GlideApp;
import me.weey.graduationproject.client.smessager.service.LoginHandlerService;
import me.weey.graduationproject.client.smessager.utils.CommonUtil;
import me.weey.graduationproject.client.smessager.utils.Constant;
import me.weey.graduationproject.client.smessager.utils.MPermissionUtils;
import me.weey.graduationproject.client.smessager.utils.SPUtil;
import me.weey.graduationproject.client.smessager.utils.UIUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 设置的Activity
 * Created by weikai on 2018/03/28/0028.
 */
public class SettingActivity extends AppCompatActivity {
    private static final String TAG = "SettingActivity";

    @BindView(R.id.app_bar_setting) AppBarLayout mAppBarLayout;
    @BindView(R.id.tb_setting) Toolbar mToolBar;
    @BindView(R.id.fb_edit_personal) FloatingActionButton mFloatingEdit;
    @BindView(R.id.collapse_tool_bar_setting) CollapsingToolbarLayout mCollapse;
    @BindView(R.id.iv_avatar_setting) ImageView mAvatar;
    @BindView(R.id.tv_setting_title) TextView mEmail;
    @BindView(R.id.iv_gender_setting) ImageView mImageGender;
    @BindView(R.id.tv_setting_gender) TextView mGender;
    @BindView(R.id.tv_setting_bio) TextView mBio;

    private User mUser;
    private File mAvatarImg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //获取主题的颜色
        int color = UIUtil.getThemeColorAccent(SettingActivity.this);
        //设置沉浸式
        UIUtil.setTranslucentStatus(getWindow(), color);
        setContentView(R.layout.activity_setting);
        //绑定UI
        ButterKnife.bind(this);
        //取出SP中的数据
        if (!initData()) {
            return;
        }
        //初始化UI
        initUI();
    }

    /**
     * 取出SP中的相关信息
     */
    private Boolean initData() {
        String userJSON = SPUtil.getString(this, Constant.USER_INFO);
        if (!TextUtils.isEmpty(userJSON)) {
            //转换JSON
            mUser = JSON.parseObject(userJSON, User.class);
            if (mUser != null) {
                Log.i(TAG, "initData: 取到了SP中的用户信息" + mUser.toString());
            }
        }
        //处理头像
        initAvatar();
        if (mUser != null) {
            return true;
        } else {
            Log.i(TAG, "initData: 没取到用户信息");
            return false;
        }
    }

    /**
     * 对头像的处理
     */
    private void initAvatar() {
        //判断图片是否已经下载
        mAvatarImg = new File(getFilesDir() + "avatar.png");
        if (!mAvatarImg.exists()) {
            //下载图片
            Request request = new Request.Builder().url(Constant.SERVER_ADDRESS + "account/avatars/" + mUser.getId()).build();
            CommonUtil.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Bitmap decodeStream = BitmapFactory.decodeStream(response.body().byteStream());
                    //保存
                    decodeStream.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(mAvatarImg));
                    runOnUiThread(() -> {
                        //更新头像
                        GlideApp.with(mAvatar.getContext())
                                .load(mAvatarImg)
                                .fitCenter()
                                .placeholder(R.color.light_gray)
                                .error(R.color.light_gray)
                                .into(mAvatar);
                    });
                }
            });
        }
    }

    /**
     * 初始化UI
     */
    private void initUI() {
        //设置ToolBar
        setSupportActionBar(mToolBar);
        if (getSupportActionBar() == null) return;
        //显示左上角返回键
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //给返回键设置点击事件
        mToolBar.setNavigationOnClickListener((v) -> onBackPressed());
        //设置标题的名称
        mCollapse.setTitle(mUser.getUserName());
        //用Glide显示图片
        GlideApp.with(mAvatar.getContext())
                .load(mAvatarImg)
                .fitCenter()
                .placeholder(R.color.light_gray)
                .error(R.color.light_gray)
                .into(mAvatar);
        //显示邮箱和性别
        mEmail.setText(mUser.getEmail());
        switch (mUser.getGender()) {
            case 0:
                mImageGender.setImageResource(R.drawable.gender_man);
                mGender.setText(R.string.secret);
                break;
            case 1:
                mImageGender.setImageResource(R.drawable.gender_man);
                mGender.setText(R.string.male);
                break;
            case 2:
                mImageGender.setImageResource(R.drawable.gender_female);
                mGender.setText(R.string.female);
        }
        //显示个性签名
        if (TextUtils.isEmpty(mUser.getBio())) {
            mBio.setText("None");
        } else {
            mBio.setText(mUser.getBio());
        }
    }

    /**
     * 性别修改相关
     */
    @OnClick(R.id.ll_item_gender)
    public void clickGender(View view) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        //引入View
        View inflate = getLayoutInflater().inflate(R.layout.sheet_gender_dialog, null);
        //设置Dialog
        bottomSheetDialog.setContentView(inflate);
        //显示
        bottomSheetDialog.show();
        //注册点击事件
        inflate.findViewById(R.id.tv_gender_secret).setOnClickListener(v -> {
            //性别保密
            if (mUser.getGender() != 0) {
                updatePersonalInfo(Constant.INFO_TYPE_GENDER, String.valueOf(0));
            }
            //隐藏
            bottomSheetDialog.dismiss();
        });
        inflate.findViewById(R.id.tv_gender_female).setOnClickListener(v -> {
            //性别女
            if (mUser.getGender() != 2) {
                updatePersonalInfo(Constant.INFO_TYPE_GENDER, String.valueOf(2));
            }
            //隐藏
            bottomSheetDialog.dismiss();
        });
        inflate.findViewById(R.id.tv_gender_male).setOnClickListener(v -> {
            //性别男
            if (mUser.getGender() != 1) {
                updatePersonalInfo(Constant.INFO_TYPE_GENDER, String.valueOf(1));
            }
            //隐藏
            bottomSheetDialog.dismiss();
        });
    }

    /**
     * 点击相册的事件
     */
    @OnClick(R.id.iv_avatar_setting)
    public void clickAvatar(ImageView imageView) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        //引入View
        View inflate = getLayoutInflater().inflate(R.layout.sheet_avatar_dialog, null);
        //设置Dialog
        bottomSheetDialog.setContentView(inflate);
        //显示
        bottomSheetDialog.show();
        //注册点击事件
        inflate.findViewById(R.id.ll_open_avatar).setOnClickListener(v -> {
            //打开大图
            Intent bigImageIntent = new Intent();
            bigImageIntent.setClass(SettingActivity.this, BigImageActivity.class);
            bigImageIntent.putExtra(BigImageActivity.BIG_IMAGE_PATH, mAvatarImg.getAbsolutePath());
            startActivity(bigImageIntent);
            bottomSheetDialog.dismiss();
        });

        inflate.findViewById(R.id.ll_open_camera).setOnClickListener(v -> {
            //打开相机取图，申请权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //大于M的系统
                askCameraPermission(imageView);
            } else {
                startTake();
            }
            bottomSheetDialog.dismiss();
        });
    }

    /**
     * 申请拍照的权限
     */
    private void askCameraPermission(ImageView imageView) {
        MPermissionUtils.requestPermissionsResult(SettingActivity.this, Constant.PERMISSION_CAMERA,
                new String[]{Manifest.permission.CAMERA}, new MPermissionUtils.OnPermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        //授权成功
                        startTake();
                    }

                    @Override
                    public void onPermissionDenied() {
                        //当权限拒绝的时候
                        MPermissionUtils.showTipsDialog(SettingActivity.this);
                    }
                });
    }

    /**
     * 拍照方法
     */
    private void startTake() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //判断是否有相机应用
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            //创建临时图片文件
            if (mAvatarImg.exists()) {
                mAvatarImg.delete();
            }
            if (!mAvatarImg.getParentFile().exists()) {
                mAvatarImg.getParentFile().mkdirs();
            }
            try {
                mAvatarImg.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //设置Action为拍照
            takePictureIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
            //这里加入flag
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri photoURI = FileProvider.getUriForFile(this, "me.weey.graduationproject.client.smessager.fileprovider", mAvatarImg);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, 5003);
        }
    }

    /**
     * 启动其他Activity返回的数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 5003) {
            //调用更新
            Log.i(TAG, "onActivityResult: 获取到拍照图片，图片的路径：" + mAvatarImg.getAbsolutePath());
            updatePersonalInfo(Constant.INFO_TYPE_AVATAR, mAvatarImg.getAbsolutePath());
        }
    }

    /**
     * 点击更新个人简介
     * @param view
     */
    @OnClick(R.id.ll_item_bio)
    public void clickBio(View view) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View inflate = getLayoutInflater().inflate(R.layout.sheet_bio_dialog, null);
        //初始化
        EditText bioText = inflate.findViewById(R.id.et_setting_bio);
        bioText.setHint(R.string.bio);
        //设置默认
        if (null == mUser.getBio()) {
            bioText.setText("");
        } else {
            bioText.setText(mUser.getBio().trim());
        }
        //设置字数
        TextView words = inflate.findViewById(R.id.tv_setting_bio_words);
        words.setVisibility(View.VISIBLE);
        words.setText(bioText.getText().toString().length() + "/140");
        dialog.setContentView(inflate);
        //显示dialog
        dialog.show();
        //注册字数改变的字数
        bioText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //获取当前的字数
                words.setText(bioText.getText().toString().length() + "/140");
            }
        });
        //dismiss以后就更新数据
        dialog.setOnDismissListener(dialog1 -> {
            //判断当前输入框的内容和原先是否一致
            if (bioText.getText().toString().equals(mUser.getBio()) || (bioText.getText().toString().trim().equals("") && mUser.getBio() == null)) {
               //没有更改
            } else {
                //有更改信息
                updatePersonalInfo(Constant.INFO_TYPE_BIO, bioText.getText().toString().trim());
            }
        });
    }

    /**
     * 修改密码
     */
    @OnClick(R.id.ll_change_pwd)
    public void clickChangePassword(View view) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View inflate = getLayoutInflater().inflate(R.layout.sheet_change_password_dialog, null);
        //获取相关的控件
        EditText originPwd = inflate.findViewById(R.id.et_setting_password_original);
        EditText newPwd = inflate.findViewById(R.id.et_setting_password_new);
        EditText repeatPwd = inflate.findViewById(R.id.et_setting_password_repeat);
        Button change = inflate.findViewById(R.id.bt_setting_change_password);
        //设置点击事件
        change.setOnClickListener(v -> {
            //判断原始密码与现在的密码是否一致
            String truePwd = mUser.getPassword();
            if (!originPwd.getText().toString().trim().equals(truePwd)) {
                RxToast.warning(getResources().getString(R.string.origin_password_error));
                return;
            }
            //密码不变
            if (originPwd.getText().toString().equals(newPwd.getText().toString())) {
                dialog.cancel();
                return;
            }
            //判断新密码是否一致
            if (!newPwd.getText().toString().equals(repeatPwd.getText().toString())) {
                RxToast.warning(getResources().getString(R.string.password_repeat_password_not_same));
                return;
            }
            //请求HTTP请求
            updatePersonalInfo(Constant.INFO_TYPE_PASSWORD, newPwd.getText().toString());
            dialog.dismiss();
        });


        dialog.setContentView(inflate);
        //显示dialog
        dialog.show();
    }


    /**
     * 浮动按钮的点击事件
     */
    @OnClick(R.id.fb_edit_personal)
    public void clickFloatButton(FloatingActionButton view) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View inflate = getLayoutInflater().inflate(R.layout.sheet_bio_dialog, null);
        //找到相应控件修改
        EditText username = inflate.findViewById(R.id.et_setting_bio);
        username.setHint(R.string.Username);
        TextView words = inflate.findViewById(R.id.tv_setting_bio_words);
        words.setVisibility(View.GONE);
        //显示姓名
        username.setText(mUser.getUserName());
        dialog.setContentView(inflate);
        //显示dialog
        dialog.show();
        //注册点击事件
        dialog.setOnDismissListener(dialog1 -> {
            if (!mUser.getUserName().equals(username.getText().toString().trim()) && !TextUtils.isEmpty(username.getText().toString().trim())) {
                //更新
                updatePersonalInfo(Constant.INFO_TYPE_NAME, username.getText().toString().trim());
            }
        });
    }

    /**
     * 点击注销
     * @param view
     */
    @OnClick(R.id.bt_setting_logout)
    public void clickLogout(View view) {
        //集合清空
        Constant.getFriendsListInstant().clear();
        Constant.getProcessMapInstant().clear();
        Constant.getAesKeyMapInstant().clear();
        Constant.getMyPrivateKeyMapInstant().clear();
        Constant.getMyPublicKeyMapInstant().clear();
        Constant.getFriendPublicKeyMapInstant().clear();
        Constant.getfriendRandomMapInstant().clear();
        Constant.getonlineStatusRandomMapInstant().clear();
        //修改为第一次登录
        SPUtil.saveBoolean(this, Constant.FIRST_BOOT, true);
        Intent intent = new Intent(SettingActivity.this, LoginHandlerService.class);
        stopService(intent);
        //清空SP
        SPUtil.saveString(SettingActivity.this, Constant.USER_INFO, null);
        //发送广播
        Intent intentBroadcast = new Intent(MainActivity.LOGOUT_BROADCAST);
        sendBroadcast(intentBroadcast);

        finish();
    }

    /**
     * 使用OKHTTP发送更新个人信息的请求
     */
    private void updatePersonalInfo(Integer infoType, String content) {
        //计算token
        String token = RxEncryptTool.encryptSHA256ToString(mUser.getId() + Constant.SERVER_PUBLIC_KEY + "token").toLowerCase();
        Log.i(TAG, "updatePersonalInfo: token:" + token);
        //判断请求类型
        RequestBody requestBody = null;
        if (infoType.equals(Constant.INFO_TYPE_AVATAR)) {
            //需要多文件上传
            requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("avatar", "avatar", RequestBody.create(MediaType.parse("application/octet-stream"), new File(content)))
                    .addFormDataPart("infoType", String.valueOf(infoType))
                    .addFormDataPart("token", token)
                    .addFormDataPart("infoContent", "avatar")
                    .addFormDataPart("userID", mUser.getId())
                    .build();
        } else {
            //普通文本
            requestBody = new FormBody.Builder()
                    .add("infoType", String.valueOf(infoType))
                    .add("token", token)
                    .add("infoContent", content)
                    .add("userID", mUser.getId())
                    .build();
        }
        //构建请求体
        Request request = new Request.Builder()
                .url(Constant.SERVER_ADDRESS + "account/updateInfo")
                .post(requestBody)
                .build();
        CommonUtil.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "onFailure: 请求失败了！", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //删除文件
                new File(content).delete();
                if (!response.isSuccessful()) {
                    Log.e(TAG, "onResponse: 请求失败！");
                } else {
                    //获取结果
                    String result = response.body().string().trim();
                    //json解析
                    HttpResponse httpResponse = JSON.parseObject(result, HttpResponse.class);
                    switch (httpResponse.getStatusCode()) {
                        case Constant.CODE_FAILURE:
                        case Constant.CODE_CHECK_FAILURE:
                            RxToast.error(httpResponse.getMessage());
                            break;
                        case Constant.CODE_SUCCESS:
                            //成功以后更新SP
                            RxToast.success("更新成功！");
                            switch (infoType) {
                                case Constant.INFO_TYPE_AVATAR:
                                    //更新了新头像
                                    //清除本地缓存
                                    GlideApp.get(mAvatar.getContext()).clearDiskCache();
                                    runOnUiThread(() -> {
                                        //清除内存缓存
                                        GlideApp.get(mAvatar.getContext()).clearMemory();
                                        //重新加载图片
                                        GlideApp.with(mAvatar.getContext())
                                                .load(Constant.SERVER_ADDRESS + "account/avatars/" + mUser.getId())
                                                .fitCenter()
                                                .placeholder(R.color.light_gray)
                                                .error(R.color.light_gray)
                                                .into(mAvatar);
                                    });
                                    break;
                                case Constant.INFO_TYPE_GENDER:
                                    //更新了性别
                                    mUser.setGender(Short.valueOf(content.trim()));
                                    SPUtil.saveString(SettingActivity.this, Constant.USER_INFO, JSON.toJSONString(mUser));
                                    switch (content) {
                                        case "0":
                                            mGender.setText(R.string.secret);
                                            break;
                                        case "1":
                                            mGender.setText(R.string.male);
                                            break;
                                        case "2":
                                            mGender.setText(R.string.female);
                                            break;
                                    }
                                    break;
                                case Constant.INFO_TYPE_BIO:
                                    //更新了个人简介
                                    mUser.setBio(content);
                                    SPUtil.saveString(SettingActivity.this, Constant.USER_INFO, JSON.toJSONString(mUser));
                                    //更新显示
                                    if (TextUtils.isEmpty(content)) {
                                        mBio.setText("None");
                                    } else {
                                        mBio.setText(content);
                                    }
                                    break;
                                case Constant.INFO_TYPE_NAME:
                                    //更新了个人名称
                                    mUser.setUserName(content);
                                    SPUtil.saveString(SettingActivity.this, Constant.USER_INFO, JSON.toJSONString(mUser));
                                    //更新显示
                                    mCollapse.setTitle(mUser.getUserName());
                                    break;
                                case Constant.INFO_TYPE_PASSWORD:
                                    //更新密码
                                    mUser.setPassword(content);
                                    SPUtil.saveString(SettingActivity.this, Constant.USER_INFO, JSON.toJSONString(mUser));
                                    break;
                            }
                            break;
                    }
                }
            }
        });
    }
}
