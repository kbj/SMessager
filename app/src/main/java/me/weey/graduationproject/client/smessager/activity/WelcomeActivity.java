package me.weey.graduationproject.client.smessager.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.vondear.rxtools.RxActivityTool;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.utils.Constant;
import me.weey.graduationproject.client.smessager.utils.SPUtil;

/**
 * 首次进入的欢迎页
 * Created by weikai on 2018/01/27/0027.
 */

public class WelcomeActivity extends AppCompatActivity {

    @BindView(R.id.bt_start) Button button;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //判断是否是第一次启动，是的话跳转到引导页
        Boolean isFirst = SPUtil.getBoolean(this, Constant.FIRST_BOOT, true);
        if (!isFirst) {
            //不是第一次启动
            RxActivityTool.skipActivityAndFinish(this, MainActivity.class);
            return;
        }
        //设置成沉浸式
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        setContentView(R.layout.activity_welcome);
        ButterKnife.bind(WelcomeActivity.this);

        //按钮的点击事件
        button.setOnClickListener(v -> {
            //跳转到注册的Activity
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
