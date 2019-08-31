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
 * @Author wonderful
 * @Description 添加朋友，同时发起好友申请
 * @Date 2019-8-30
 */
public class AddFriend extends HttpServlet{
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        PrintWriter out = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        
        try {
            response.setContentType("text/plain; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            out = response.getWriter();
            String account = request.getParameter("account");
            String friendAccount = request.getParameter("friendAccount");
            
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
              
            if(account.equals(friendAccount)){
                String querySql = buildSqlExist(account,friendAccount);
                result = statement.executeQuery(querySql);
                if(result.next()){
                    out.print("exist");
                    return;
                }
                if(addFriend(account,friendAccount)){
                    out.print("success");
                    return;
                }else{
                    out.print("fail");
                    return;
                }
            }
            
            int situation = situationAnalyze(account,friendAccount);
            switch(situation){
                case 0:
                    //删除双方请求
                    removeRequest(friendAccount,account);
                    removeRequest(account,friendAccount);
                    out.print("exist");
                    break;
                case 1:
                    //删除请求再添加请求，为解决A+B,B未处理，A-B,B同意，B+A，A收不到数据,AB为好友，A-B,B+A,A收不到数据
                    addRuquest(account,friendAccount);
                    out.print("exist");
                    break;
                case 2:
                    if(addFriend(account,friendAccount)){
                        out.print("success");
                        removeRequest(friendAccount,account);
                        removeRequest(account,friendAccount);
                    }else{
                        out.print("fail");
                    }
                    break;
                case 3:
                    if(addFriend(account,friendAccount)){
                        out.print("success");
                        addRuquest(account,friendAccount);
                    }else{
                        out.print("fail");
                    }
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
            Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, ex);
        } 
        catch (SQLException ex) {
            if(out != null){
                out.print(ex.getMessage());
            }
            Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, ex);
        } 
        finally {
            DBCPUtils.closeAll(result, statement, connection);
            if(out != null){
                out.close();
            }
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){

    }
    
    /**
    * @description 删除请求数据
    * @param account
    * @param host
    * @return boolean
    */
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
            Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            DBCPUtils.closeAll(result, statement, connection);
        }
        
        return userModel;
    }
    
    /**
     * @Description 添加好友请求，数据库方案
     * @param account
     * @param friendAccount
     * @return boolean
     */
    private boolean addRuquest(String account,String friendAccount){
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        try {
            UserModel userModel = getUserModel(account);
            if(userModel == null)return false;
            FriendRequestModel requestModel = new FriendRequestModel();
            requestModel.changeToFriendRequestModel(userModel);
            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:HH:ss");
            String time = format.format(date);        
            requestModel.setRequestTime(time);
        
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
            
            //先删除数据，A+B,B未处理时A-B,A+B,就会出现多条数据，这里选择删除再添加是为了更新时间
            String updateSql = buildDeleteSql(account,friendAccount);
            statement.executeUpdate(updateSql);
            
            updateSql = buildInsertSql(friendAccount,requestModel);
            int rows = statement.executeUpdate(updateSql);
            return rows>0;
        } catch (SQLException ex) {
            Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            DBCPUtils.closeAll(result, statement, connection);
        }
        
        return false;
    }
    
    
    //写入请求（文件系统）
    private boolean fileWrite(File file,HashMap<String,FriendRequestModel> requestMap) {
        RandomAccessFile randomAccessFile = null;
        FileChannel channel = null;
        FileLock lock = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            channel = randomAccessFile.getChannel();
            while (true) {
                try {
                   lock = channel.tryLock();
                   break;
                } catch (Exception e) {
                    //Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, e);
                }
            }

            Gson gson = new Gson();
            String jsonData = gson.toJson(requestMap);
            randomAccessFile.writeBytes(jsonData);
        } catch (Exception e) {
            Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, e);
            return false;
        } finally {
            try {
                if(lock != null) {
                    lock.release();
                }
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                if (channel != null) {
                    channel.close();
                }
            } catch (IOException e) {
                Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        
        return true;
    }
    
    //读取请求（文件系统）
    private HashMap<String,FriendRequestModel> fileRead(File file) {
        HashMap<String,FriendRequestModel> requestMap = null;
        RandomAccessFile randomAccessFile = null;
        FileChannel channel = null;
        FileLock lock = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            channel = randomAccessFile.getChannel();
            while (true) {
                try {
                    lock = channel.tryLock();
                    break;
                } catch (Exception e) {
                    //Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, e);
                }
            }

            StringBuilder builder = new StringBuilder();
            String jsonData;
            while((jsonData = randomAccessFile.readLine()) != null){
                builder.append(jsonData);
            }
            Gson gson = new Gson();
            Type type = new TypeToken<HashMap<String,FriendRequestModel>>(){}.getType();
            requestMap = gson.fromJson(builder.toString(), type);

        } catch (Exception e) {
            Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, e);
        }finally {
            try {
                if(lock != null) {
                    lock.release();
                }
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                if (channel != null) {
                    channel.close();
                }
            } catch (IOException e) {
                Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        
        return requestMap;
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

            //添加到FRIENDLIST表中
            String updateSql = buildSqlInsert(account,userModel);
            int rows = statement.executeUpdate(updateSql);
            return rows >0;
        } catch (SQLException ex) {
            Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            DBCPUtils.closeAll(result, statement, connection);
        }
        
        return false;
    }
    
    /**
     * @Description 好友情况判断(A+B)，返回值：0:B->A,A->B;1:B->A,A-!B;2:B-!A,A->B;3:B-!A,A-!B;-1:other
     * @param account
     * @param friendAccount
     * @return int
     */
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
            Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            DBCPUtils.closeAll(result, statement, connection);
        }
        return situation;
    }
    
    /**
    * @description 朋友查询语句构造函数，account:查询账号，foreignkey:外键
    * @param account
    * @param foreignkey
    * @return String
    */
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
    
    //请求查询语句构造函数，查询好友申请是否已经存在，account:查询账号，host:接收者
    private String buildRequestExistSql(String account,String host){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select * from ");
        stringBuilder.append(CommonConstant.FRIEND_REQUEST_PATH);
        stringBuilder.append(" where host = '");
        stringBuilder.append(host);
        stringBuilder.append("' and account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
    private String buildInsertSql(String host,FriendRequestModel requestModel){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("insert into ");
        stringBuilder.append(CommonConstant.TABLE_FRIEND_REQUEST);
        stringBuilder.append(" values('");
        stringBuilder.append(requestModel.getAccount());
        stringBuilder.append("',");
        if(requestModel.getNickname() == null){
            stringBuilder.append("null,");
        }else{
            stringBuilder.append("'");
            stringBuilder.append(requestModel.getNickname());
            stringBuilder.append("',");
        }
        if(requestModel.getLifeMotto() == null){
            stringBuilder.append("null,");
        }else{
            stringBuilder.append("'");
            stringBuilder.append(requestModel.getLifeMotto());
            stringBuilder.append("',");
        }
        if(requestModel.getImageUrl() == null){
            stringBuilder.append("null,");
        }else{
            stringBuilder.append("'");
            stringBuilder.append(requestModel.getImageUrl());
            stringBuilder.append("',");
        }
        if(requestModel.getRequestTime() == null){
            stringBuilder.append("null,'");
        }else{
            stringBuilder.append("'");
            stringBuilder.append(requestModel.getRequestTime());
            stringBuilder.append("','");
        }
        stringBuilder.append(host);
        stringBuilder.append("')");
        
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
    
    private String buildSqlExist(String account,String friendAccount){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select * from ");
        stringBuilder.append(CommonConstant.TABLE_FRIEND_LIST);
        stringBuilder.append(" where foreignkey = '");
        stringBuilder.append(account);
        stringBuilder.append("' and account = '");
        stringBuilder.append(friendAccount);
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
