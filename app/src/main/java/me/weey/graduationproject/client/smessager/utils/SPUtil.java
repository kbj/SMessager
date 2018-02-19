package me.weey.graduationproject.client.smessager.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences相关工具类
 * Created by weikai on 2017/7/27/0027.
 */

public class SPUtil {

    private static final String XML_FILE_NAME = "cache";

    /**
     * 使用SharedPreferences保存String数据
     * @param context 上下文对象
     * @param title   保存的key值
     * @param content  key对应的value值
     */
    public static void saveString(Context context, String title, String content) {
        SharedPreferences preferences = context.getSharedPreferences(XML_FILE_NAME, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putString(title, content);
        //后台写入
        edit.apply();
    }

    /**
     * 通过Key得到Value
     * @param context
     * @param title
     * @return
     */
    public static String getString(Context context, String title) {
        SharedPreferences preferences = context.getSharedPreferences(XML_FILE_NAME, Context.MODE_PRIVATE);
        return preferences.getString(title, "");
    }

    public static void saveInt(Context context, String title, Integer content) {
        SharedPreferences preferences = context.getSharedPreferences(XML_FILE_NAME, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putInt(title, content);
        //后台写入
        edit.apply();
    }

    public static Integer getInt(Context context, String title, Integer def) {
        SharedPreferences preferences = context.getSharedPreferences(XML_FILE_NAME, Context.MODE_PRIVATE);
        return preferences.getInt(title, def);
    }

    public static void saveLong(Context context, String title, Long content) {
        SharedPreferences preferences = context.getSharedPreferences(XML_FILE_NAME, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putLong(title, content);
        //后台写入
        edit.apply();
    }

    public static Long getLong(Context context, String title, Long def) {
        SharedPreferences preferences = context.getSharedPreferences(XML_FILE_NAME, Context.MODE_PRIVATE);
        return preferences.getLong(title, def);
    }

    public static Boolean getBoolean(Context context, String title, Boolean def) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(XML_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(title, def);
    }

    public static void saveBoolean(Context context, String title, Boolean content) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(XML_FILE_NAME, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putBoolean(title, content);
        //后台写入
        edit.apply();
    }
}
