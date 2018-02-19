package me.weey.graduationproject.client.smessager.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天首页消息列表结构
 * Created by weikai on 2018/02/06/0006.
 */

public class ChatList implements Serializable {
    //这个用户对应的ID
    private String userId;
    //聊天头像的图片下载地址
    private String avatarUrl;
    //用户名
    private String userName;
    //最新消息
    private String latestMessage;
    //上面那条消息对应的发送或者接收时间
    private Date time;
    //未读消息的计数
    private boolean isNewMessage;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getLatestMessage() {
        return latestMessage;
    }

    public void setLatestMessage(String latestMessage) {
        this.latestMessage = latestMessage;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public boolean isNewMessage() {
        return isNewMessage;
    }

    public void setNewMessage(boolean newMessage) {
        isNewMessage = newMessage;
    }

    public ChatList() {
    }

    public ChatList(String userId, String avatarUrl, String userName, String latestMessage, Date time, boolean isNewMessage) {
        this.userId = userId;
        this.avatarUrl = avatarUrl;
        this.userName = userName;
        this.latestMessage = latestMessage;
        this.time = time;
        this.isNewMessage = isNewMessage;
    }

    @Override
    public String toString() {
        return "ChatList{" +
                "userId='" + userId + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", userName='" + userName + '\'' +
                ", latestMessage='" + latestMessage + '\'' +
                ", time=" + time +
                ", isNewMessage=" + isNewMessage +
                '}';
    }
}
