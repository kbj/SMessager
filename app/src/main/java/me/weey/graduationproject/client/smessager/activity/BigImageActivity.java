package me.weey.graduationproject.client.smessager.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.weey.graduationproject.client.smessager.R;
import me.weey.graduationproject.client.smessager.widget.PinchImageView;

/**
 * 浏览大图的Activity
 * Created by weikai on 2018/03/13/0013.
 */

public class BigImageActivity extends AppCompatActivity {

    public static final String BIG_IMAGE_PATH = "bigImage";

    @BindView(R.id.piv_image) PinchImageView mBigImage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //设置成沉浸式
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        setContentView(R.layout.activity_big_image);
        ButterKnife.bind(BigImageActivity.this);
        initUI();
        //获取传来的图片参数
        Intent intent = getIntent();
        String imgPath = intent.getStringExtra(BIG_IMAGE_PATH);
        //校验
        Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
        if (bitmap != null) {
            //显示图片
            mBigImage.setImageBitmap(bitmap);
        } else {
            //参数为空，显示默认全黑
            mBigImage.setImageResource(R.color.black);
        }
    }

    private void initUI() {
        //设置背景黑色
        mBigImage.setBackgroundColor(Color.BLACK);
    }

    @OnClick(R.id.piv_image)
    public void clickImage() {
        finish();
    }
}
