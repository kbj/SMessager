package me.weey.graduationproject.client.smessager.entity;

import java.util.Arrays;

/**
 * 消息的类型
 * Created by weikai on 2018/02/17/0017.
 */

public class Msg {
    private Integer msgType;
    private byte[] message;

    public Integer getMsgType() {
        return msgType;
    }

    public void setMsgType(Integer msgType) {
        this.msgType = msgType;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Msg{" +
                "msgType=" + msgType +
                ", message=" + Arrays.toString(message) +
                '}';
    }
}
