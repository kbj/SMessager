package me.weey.graduationproject.client.smessager.entity;


import java.util.Date;

/**
 * 发送和接收数据包中的数据部分的结构
 * Created by WeiKai on 2018/01/22.
 */
public class DataStructure {
    private Date time;              //时间
    private String fromId;          //发送方的ID
    private String toID;            //这条消息发送的对象的ID
    private Integer modelType;      //这条消息所属于的模块
    private Integer messageType;    //消息的类型
    private String message;         //要发送的消息
    private Integer process;        //记录当前是加密通讯协议中的第几个流程

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getToID() {
        return toID;
    }

    public void setToID(String toID) {
        this.toID = toID;
    }

    public Integer getModelType() {
        return modelType;
    }

    public void setModelType(Integer modelType) {
        this.modelType = modelType;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getProcess() {
        return process;
    }

    public void setProcess(Integer process) {
        this.process = process;
    }

    @Override
    public String toString() {
        return "DataStructure{" +
                "time=" + time +
                ", fromId='" + fromId + '\'' +
                ", toID='" + toID + '\'' +
                ", modelType=" + modelType +
                ", messageType=" + messageType +
                ", message='" + message + '\'' +
                ", process=" + process +
                '}';
    }

    public DataStructure(Date time, String fromId, String toID, Integer modelType, Integer messageType, String message, Integer process) {
        this.time = time;
        this.fromId = fromId;
        this.toID = toID;
        this.modelType = modelType;
        this.messageType = messageType;
        this.message = message;
        this.process = process;
    }

    public DataStructure() {
    }
}
