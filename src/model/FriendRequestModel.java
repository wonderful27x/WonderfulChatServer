/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

/**
 *
 * @author Acer
 */
public class FriendRequestModel extends UserModel{

    private String requestTime;
    
    public void changeToFriendRequestModel(UserModel userModel){
        this.setAccount(userModel.getAccount());
        this.setNickname(userModel.getNickname());
        this.setLifeMotto(userModel.getLifeMotto());
        this.setImageUrl(userModel.getImageUrl());
    }

    public String getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(String requestTime) {
        this.requestTime = requestTime;
    }
}


