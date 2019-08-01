/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderful;

import CommonConstant.CommonConstant;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HttpUserModel;
import model.UserModel;
import utils.DBCPUtils;

/**
 *
 * @author Acer
 */
public class AddFriend extends HttpServlet{
    
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        PrintWriter out = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        
        UserModel userModel = new UserModel();
        
        try {
            response.setContentType("text/plain; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            out = response.getWriter();
            String account = request.getParameter("account");
            String friendAccount = request.getParameter("friendAccount");
            
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
            
//            String querySql = "select account from " + CommonConstant.TABLE_FRIEND_LIST + " where foreignkey = '" + account + "'";
            String querySql = buildSqlExist(account);
            result = statement.executeQuery(querySql);
            while(result.next()){
                if(result.getString("account").equals(friendAccount)){
                    out.print("exist");
                    return;
                }
            }
            
//            querySql = "select * from " + CommonConstant.TABLE_USER + " where account = '" + friendAccount + "'";
            querySql = buildSqlUser(friendAccount);
            result = statement.executeQuery(querySql);

            if(result.next()){
                userModel.setAccount(result.getString("account"));
                userModel.setNickname(result.getString("nickname"));
                userModel.setLifeMotto(result.getString("lifemotto"));
                userModel.setImageUrl(result.getString("imageurl"));
            }else{
                out.print("fail");
                return;
            }
            
//            String updateSql = "insert into " + CommonConstant.TABLE_FRIEND_LIST + " values('" +
//                    userModel.getAccount() + "','" + userModel.getNickname() + "','" +
//                    "null" + "','" + userModel.getLifeMotto() + "','" + userModel.getImageUrl() +
//                    "','" + account + "')";
            String updateSql = buildSqlInsert(account,userModel);
            int rows = statement.executeUpdate(updateSql);
            if(rows >0){
                out.print("success");
            }else{
                out.print("fail");
            }
            
        } catch (IOException ex) {
            out.print(ex.toString());
            Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            out.print(ex.toString());
            Logger.getLogger(AddFriend.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            DBCPUtils.closeAll(result, statement, connection);
            if(out != null){
                out.close();
            }
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response){

    }
    
    private String buildSqlExist(String account){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select account from ");
        stringBuilder.append(CommonConstant.TABLE_FRIEND_LIST);
        stringBuilder.append(" where foreignkey = '");
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
