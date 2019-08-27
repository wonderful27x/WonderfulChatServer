/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderful;

import CommonConstant.CommonConstant;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.FriendRequestModel;
import model.UserModel;
import utils.DBCPUtils;

/**
 *
 * @author Acer
 */
public class FriendRequestDeal extends HttpServlet{
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        PrintWriter out = null;
        try {
            response.setContentType("text/plain; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            out = response.getWriter();
            String account = request.getParameter("account");
            String friendAccount = request.getParameter("friendAccount");
            String type = request.getParameter("type");
            
            int situation = situationAnalyze(account,friendAccount);
            switch(situation){
                case 0:
                    //双方已是好友，删除双方请求，暂时没有此情况
                    out.print("exist");
                    removeRequest(friendAccount,account);
                    removeRequest(account,friendAccount);
                    break;
                case 1:
                    //暂时没有此情况
                    out.print("exist");
                    removeRequest(friendAccount,account);
                    break;
                case 2:
                    //正常情况，A+B,B->A,A-!B
                    if("refuse".equals(type)){
                        removeRequest(friendAccount,account);
                        out.print("success");
                        break;
                    }
                    if(addFriend(account,friendAccount)){
                        out.print("success");
                        removeRequest(friendAccount,account);
                        removeRequest(account,friendAccount);
                    }else{
                        out.print("fail");
                    }
                    break;
                case 3:
                    //双方都不是好友，有可能A+B,A-B
                    removeRequest(friendAccount,account);
                    out.print("invalid");
                    break;
                case -1:
                    out.print("fail");
                    break;
                default:
                    out.print("fail");
                    break;
            }
        } catch (IOException ex) {
            if(out != null){
                out.print(ex.getMessage());
            }
            Logger.getLogger(FriendRequestDeal.class.getName()).log(Level.SEVERE, null, ex);
        } 
        finally {
            if(out != null){
                out.close();
            }
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){

    }
    
    private boolean removeRequest(String account,String host){
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        try {
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
            
            String querySql = buildDeleteSql(account,host);
            int rows = statement.executeUpdate(querySql);
            return rows>0;
        } catch (SQLException ex) {
            Logger.getLogger(FriendRequestDeal.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            DBCPUtils.closeAll(result, statement, connection);
        }
        
        return false;
    }
    
    private UserModel getUserModel(String account){
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        UserModel userModel = null;
        try {
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
            
            String querySql = buildSqlUser(account);
            result = statement.executeQuery(querySql);
            if(result.next()){
                userModel = new UserModel();
                userModel.setAccount(result.getString("account"));
                userModel.setNickname(result.getString("nickname"));
                userModel.setLifeMotto(result.getString("lifemotto"));
                userModel.setImageUrl(result.getString("imageurl"));
            }
        } catch (SQLException ex) {
            userModel = null;
            Logger.getLogger(FriendRequestDeal.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            DBCPUtils.closeAll(result, statement, connection);
        }
        
        return userModel;
    }
    
    private boolean addFriend(String account,String friendAccount){
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        try {
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
            
            //从USER表中取出好友信息
            UserModel userModel = getUserModel(friendAccount);
            if(userModel == null)return false;

            String updateSql = buildSqlInsert(account,userModel);
            int rows = statement.executeUpdate(updateSql);
            return rows >0;
        } catch (SQLException ex) {
            Logger.getLogger(FriendRequestDeal.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            DBCPUtils.closeAll(result, statement, connection);
        }
        
        return false;
    }
    
    //好友情况判断(A+B)，返回值：0:B->A,A->B;1:B->A,A-!B;2:B-!A,A->B;3:B-!A,A-!B;-1:other
    private int situationAnalyze(String account,String friendAccount){
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        String querySql = null;
        int situation = -1;
        int a = -1;
        int b = -1;
        try {
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
            
            querySql = buildExistSql(friendAccount,account);
            result = statement.executeQuery(querySql);
            if(result.next()){
                a = 1;
            }else{
                a = 0;
            }
            querySql = buildExistSql(account,friendAccount);
            result = statement.executeQuery(querySql);
            if(result.next()){
                b = 1;
            }else{
                b = 0;
            }
            
            if(a == 1 && b == 1){
                situation = 0;
            }else if(a == 1 && b != 1){
                situation = 1;
            }else if(a != 1 && b == 1){
                situation = 2;
            }else if(a != 1 && b != 1){
                situation = 3;
            }
         
        } catch (SQLException ex) {
            Logger.getLogger(FriendRequestDeal.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            DBCPUtils.closeAll(result, statement, connection);
        }
        return situation;
    }
    
    //朋友查询语句构造函数，account:查询账号，foreignkey:外键
    private String buildExistSql(String account,String foreignkey){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select * from ");
        stringBuilder.append(CommonConstant.TABLE_FRIEND_LIST);
        stringBuilder.append(" where account = '");
        stringBuilder.append(account);
        stringBuilder.append("' and foreignkey = '");
        stringBuilder.append(foreignkey);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
    private String buildDeleteSql(String account,String host){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("delete from ");
        stringBuilder.append(CommonConstant.TABLE_FRIEND_REQUEST);
        stringBuilder.append(" where host = '");
        stringBuilder.append(host);
        stringBuilder.append("' and account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
    private String buildSqlUser(String friendAccount){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select * from ");
        stringBuilder.append(CommonConstant.TABLE_USER);
        stringBuilder.append(" where account = '");
        stringBuilder.append(friendAccount);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
    private String buildSqlInsert(String account,UserModel userModel){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("insert into ");
        stringBuilder.append(CommonConstant.TABLE_FRIEND_LIST);
        stringBuilder.append(" values('");
        stringBuilder.append(userModel.getAccount());
        stringBuilder.append("',");
        if(userModel.getNickname() == null){
            stringBuilder.append("null,");
        }else{
            stringBuilder.append("'");
            stringBuilder.append(userModel.getNickname());
            stringBuilder.append("',");
        }
        if(userModel.getRemark() == null){
            stringBuilder.append("null,");
        }else{
            stringBuilder.append("'");
            stringBuilder.append(userModel.getRemark());
            stringBuilder.append("',");
        }
        if(userModel.getLifeMotto() == null){
            stringBuilder.append("null,");
        }else{
            stringBuilder.append("'");
            stringBuilder.append(userModel.getLifeMotto());
            stringBuilder.append("',");
        }
        if(userModel.getImageUrl() == null){
            stringBuilder.append("null,'");
        }else{
            stringBuilder.append("'");
            stringBuilder.append(userModel.getImageUrl());
            stringBuilder.append("','");
        }
        stringBuilder.append(account);
        stringBuilder.append("')");
        
        return stringBuilder.toString();
        
    }
}
