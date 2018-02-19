package me.weey.graduationproject.client.smessager.entity;


import java.io.Serializable;

/**
 * 用户基础信息类
 * Created by WeiKai on 2018/01/23.
 */
public class User implements Serializable {
    private String id;              //用户id
    private String userName;        //用户名
    private String password;        //密码
    private short gender;           //性别：0 —— 未设置，1 —— 男， 2 —— 女
    private String email;           //邮箱地址
    private String bio;             //个人简介
    private String registerIp;      //注册时候的IP地址
    private String registerBrand;   //注册时候用户所用手机的品牌
    private String registerModel;   //注册时候用户所用手机的型号
    private String IMEI;            //注册时候用户手机的IMEI信息

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public short getGender() {
        return gender;
    }

    public void setGender(short gender) {
        this.gender = gender;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getRegisterIp() {
        return registerIp;
    }

    public void setRegisterIp(String registerIp) {
        this.registerIp = registerIp;
    }

    public String getRegisterBrand() {
        return registerBrand;
    }

    public void setRegisterBrand(String registerBrand) {
        this.registerBrand = registerBrand;
    }

    public String getRegisterModel() {
        return registerModel;
    }

    public void setRegisterModel(String registerModel) {
        this.registerModel = registerModel;
    }

    public String getIMEI() {
        return IMEI;
    }

    public void setIMEI(String IMEI) {
        this.IMEI = IMEI;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", gender=" + gender +
                ", email='" + email + '\'' +
                ", bio='" + bio + '\'' +
                ", registerIp='" + registerIp + '\'' +
                ", registerBrand='" + registerBrand + '\'' +
                ", registerModel='" + registerModel + '\'' +
                ", IMEI='" + IMEI + '\'' +
                '}';
    }
}
