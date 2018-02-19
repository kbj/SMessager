package me.weey.graduationproject.client.smessager.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * 聊天相关的Service
 * Created by weikai on 2018/02/11/0011.
 */

public class ChatService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
