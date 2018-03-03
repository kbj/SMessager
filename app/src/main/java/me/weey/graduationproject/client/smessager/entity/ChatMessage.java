package me.weey.graduationproject.client.smessager.entity;

import java.io.Serializable;

/**
 * 消息的类型
 * Created by weikai on 2018/02/17/0017.
 */

public class ChatMessage implements Serializable {
    private String id;              //每条消息的ID
    private String userId;          //消息所属于用户的ID
    private String message;         //消息的正文，图片语音的时候为本地文件的地址
    private String time;            //消息产生的时间
    private int chatType;           //聊天的类型，0是发送，1是接收
    private int messageType;        //消息的类型，0是文本消息，1是图片消息，2是已读语音消息，3是未读语音消息
    private String voiceSecond;    //如果是语音消息的话，语音消息的秒数

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getChatType() {
        return chatType;
    }

    public void setChatType(int chatType) {
        this.chatType = chatType;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getVoiceSecond() {
        return voiceSecond;
    }

    public void setVoiceSecond(String voiceSecond) {
        this.voiceSecond = voiceSecond;
    }


    @Override
    public String toString() {
        return "ChatMessage{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", message='" + message + '\'' +
                ", time='" + time + '\'' +
                ", chatType=" + chatType +
                ", messageType=" + messageType +
                ", voiceSecond=" + voiceSecond +
                '}';
    }
}
