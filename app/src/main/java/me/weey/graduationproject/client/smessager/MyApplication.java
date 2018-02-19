package me.weey.graduationproject.client.smessager;

import android.app.Application;

import com.vondear.rxtools.RxTool;

import me.weey.graduationproject.client.smessager.utils.CommonUtil;
import me.weey.graduationproject.client.smessager.utils.Constant;
import okhttp3.OkHttpClient;


/**
 * 全局初始化的Application类
 * Created by weikai on 2018/01/31/0031.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //初始化工具类
        RxTool.init(this);
        //先请求ip库获得IP
        OkHttpClient httpClient = CommonUtil.getHttpClient();
    }
}
