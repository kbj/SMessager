package me.weey.graduationproject.client.smessager.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 读取聊天列表页面的数据库表存储
 * Created by weikai on 2018/02/07/0007.
 */

public class ChatListOpenHelper extends SQLiteOpenHelper {

    /**
     * 定义自己的OpenHelper需要实现自己的构造方法
     * @param context   上下文对象
     * @param name      数据库名
     * @param factory   游标工厂，这个是可选的，通常直接写null
     * @param version   数据库模型的版本，填写一个整数
     */
    public ChatListOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    /**
     * 表结构的初始化
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chat_message(id TEXT NOT NULL PRIMARY KEY, myId text NOT NULL, user_id text NOT NULL, message TEXT, time TEXT, chat_type integer NOT NULL, message_type integer NOT NULL, voice_second TEXT, is_new_message integer)");
        db.execSQL("CREATE INDEX user_id ON chat_message(user_id COLLATE BINARY)");

        //添加假数据
        //db.execSQL("insert into chat_list values('jkflsdfnsdklnf21e2', '', 'test', '这是一条测试信息', '2018-02-07 10:00:45', 1)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
