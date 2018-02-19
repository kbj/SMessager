package me.weey.graduationproject.client.smessager.utils;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.vondear.rxtools.RxEncryptTool;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Created by weikai on 2017/8/9/0009.
 */

public class CommonUtil {

    private static HashMap cookieStore;
    private static File mAvatarCache;
    //private static CookieJar cookieJar;

    //Cookie处理的部分
    /*static {
        if (cookieStore == null) {
            cookieStore = new HashMap<String, List<Cookie>>();
            Log.i("OkHttpUtil", "new the hashMap");
        }

        cookieJar = new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                if (cookieStore.get(url.host()) == null) {
                    //如果没有保存到Cookie的话就保存
                    for (Cookie cookie : cookies) {
                        //只需要保存有JSESSIONID的Cookie
                        if (cookie.name().equals("JSESSIONID")) {
                            Log.i("OkHttpUtil", "saveFromResponse" + cookies);
                            cookieStore.put(url.host(), cookies);
                            Log.i("hashmap", cookieStore.toString());
                            break;
                        }
                    }
                }
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                Log.i("hashmap", cookieStore.toString());
                List cookies = (List) cookieStore.get(url.host());
                if (cookies == null) {
                    Log.i("OkHttpUtil", "loadForRequest");
                } else {
                    Log.i("OkHttpUtil", "loadForRequest" + cookies);
                }
                return cookies != null ? cookies : new ArrayList();
            }
        };
    }*/


    /**
     * 压缩图片
     * @param srcBitmap BitMap路径
     * @param newSize 压缩后的图片大小
     * @return
     */
    public static Bitmap downSize(Bitmap srcBitmap, int newSize) {
        if (newSize <= 1) {
            // 如果欲縮小的尺寸過小，就直接定為128
            newSize = 128;
        }
        int srcWidth = srcBitmap.getWidth();
        int srcHeight = srcBitmap.getHeight();
        String text = "source image size = " + srcWidth + "x" + srcHeight;
        int longer = Math.max(srcWidth, srcHeight);

        if (longer > newSize) {
            double scale = longer / (double) newSize;
            int dstWidth = (int) (srcWidth / scale);
            int dstHeight = (int) (srcHeight / scale);
            srcBitmap = Bitmap.createScaledBitmap(srcBitmap, dstWidth, dstHeight, false);
            System.gc();
            text = "\nscale = " + scale + "\nscaled image size = " +
                    srcBitmap.getWidth() + "x" + srcBitmap.getHeight();
        }
        return srcBitmap;
    }

    /**
     * 返回一个单例的HttpClient对象
     * @return
     */
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            //.cookieJar(cookieJar)
            .build();
    public static OkHttpClient getHttpClient() {
        return client;
    }


    /**
     * 得到amr的时长
     */
    public static int getAmrDuration(File file) throws IOException {
        long duration = -1;
        int[] packedSize = {12, 13, 15, 17, 19, 20, 26, 31, 5, 0, 0, 0, 0, 0, 0, 0};
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            long length = file.length();//文件的长度
            int pos = 6;//设置初始位置
            int frameCount = 0;//初始帧数
            int packedPos = -1;
            /////////////////////////////////////////////////////
            byte[] datas = new byte[1];//初始数据值
            while (pos <= length) {
                randomAccessFile.seek(pos);
                if (randomAccessFile.read(datas, 0, 1) != 1) {
                    duration = length > 0 ? ((length - 6) / 650) : 0;
                    break;
                }
                packedPos = (datas[0] >> 3) & 0x0F;
                pos += packedSize[packedPos] + 1;
                frameCount++;
            }
            /////////////////////////////////////////////////////
            duration += frameCount * 20;//帧数*20
        } finally {
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
        }
        return (int) (duration / 1000);
    }

    /**
     * 获取头像缓存的路径
     */
    public static File getAvatarCacheDir(Context context) {
        if (mAvatarCache == null) {
            mAvatarCache = new File(context.getCacheDir() + "/" + Constant.AVATAR_CHCHE_DIR);
        }
        return mAvatarCache;
    }

    /**
     * 根据URL地址计算出在本地应该保存的文件名
     */
    public static String getPictureNameByUrl(String url) {
        if (TextUtils.isEmpty(url)) return "";
        return RxEncryptTool.encryptSHA1ToString(url);
    }

    /**
     * 弹出通知
     */
     public static void showNotification(Context context, Bitmap largeIcon, int smallIcon, String ticker,
                                         String title, String content, PendingIntent pendingIntent,
                                         PendingIntent pendingIntentCancel, int notifyId) {

         String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";

         NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_HIGH);

             // Configure the notification channel.
             notificationChannel.setDescription("Channel description");
             notificationChannel.enableLights(true);
             notificationChannel.setLightColor(Color.RED);
             notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
             notificationChannel.enableVibration(true);
             notificationManager.createNotificationChannel(notificationChannel);
         }


         NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                 /**设置通知左边的大图标**/
                 .setLargeIcon(largeIcon)
                 /**设置通知右边的小图标**/
                 .setSmallIcon(smallIcon)
                 /**通知首次出现在通知栏，带上升动画效果的**/
                 .setTicker(ticker)
                 /**设置通知的标题**/
                 .setContentTitle(title)
                 /**设置通知的内容**/
                 .setContentText(content)
                 /**通知产生的时间，会在通知信息里显示**/
                 .setWhen(System.currentTimeMillis())
                 /**设置该通知优先级**/
                 .setPriority(Notification.PRIORITY_HIGH)
                 /**设置这个标志当用户单击面板就可以让通知将自动取消**/
                 .setAutoCancel(true)
                 /**设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)**/
                 .setOngoing(false)
                 /**向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合：**/
                 .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                 .setContentIntent(pendingIntent).setDeleteIntent(pendingIntentCancel);


         notificationManager.notify(notifyId, builder.build());
     }
}
