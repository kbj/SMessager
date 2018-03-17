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
    //最新的消息
    private String message;
    //时间
    private Date time;
    //消息的类型，文本语音图片
    private Integer messageType;
    //是否为新消息
    private Boolean isNewMessage;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public Boolean getNewMessage() {
        return isNewMessage;
    }

    public void setNewMessage(Boolean newMessage) {
        isNewMessage = newMessage;
    }

    @Override
    public String toString() {
        return "ChatList{" +
                "userId='" + userId + '\'' +
                ", message='" + message + '\'' +
                ", time=" + time +
                ", messageType=" + messageType +
                ", isNewMessage=" + isNewMessage +
                '}';
    }
}
