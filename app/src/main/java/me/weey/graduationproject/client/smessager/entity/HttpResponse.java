package me.weey.graduationproject.client.smessager.entity;


import java.util.Date;

/**
 * HTTP请求的返回格式属性
 * Created by WeiKai on 2018/01/24.
 */
public class HttpResponse {
    private Integer statusCode;
    private String message;
    private Date time;
    private Integer messageType;

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
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

    @Override
    public String toString() {
        return "HttpResponse{" +
                "statusCode=" + statusCode +
                ", message='" + message + '\'' +
                ", time=" + time +
                ", messageType=" + messageType +
                '}';
    }
}
