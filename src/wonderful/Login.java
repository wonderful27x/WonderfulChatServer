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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HttpUserModel;
import model.UserModel;
import utils.DBCPUtils;

/**
 * @Author wonderful
 * @Description 登录，登录状态标志置1，同时返回用户信息和好友列表信息
 * @Date 2019-8-30
 */
public class Login extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {
//        doPost(request,response);
        
        response.setContentType("text/plain; charset=utf-8");
	response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
	String string = "Have a wonderful day";
	out.print(string);
	out.close();

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){
        PrintWriter out = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        
        HttpUserModel httpUserModel = new HttpUserModel();
        List<UserModel> users = new ArrayList();
        UserModel userModel = new UserModel();
        Gson gson = new Gson();
        
        try {
            response.setContentType("text/javascript; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            out = response.getWriter();
            String account = request.getParameter("account");
            String pass = request.getParameter("password");
            
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();

            String querySql = buildSqlQuery(account);
            result = statement.executeQuery(querySql);

            if(result.next()){
                String password = result.getString("password");
                if(password != null && password.equals(pass)){
                    userModel.setAccount(result.getString("account"));
                    userModel.setPassword(password);
                    userModel.setNickname(result.getString("nickname"));
                    userModel.setLifeMotto(result.getString("lifemotto"));
                    userModel.setImageUrl(result.getString("imageurl"));
                    users.add(userModel);
                    httpUserModel.setResult("success");

                    String update = buildSqlUpdate(account);
                    int rows = statement.executeUpdate(update);
                    if(rows <= 0){
                        httpUserModel.setResult("fail");
                        httpUserModel.setMessage("登录异常，稍后再试！");
                        return;
                    }
                    
                }else{
                    httpUserModel.setResult("fail");
                    httpUserModel.setMessage("密码错误！");
                }
            }else{
                httpUserModel.setResult("fail");
                httpUserModel.setMessage("账号信息不存在！");
            }
            
            querySql = buildSqlQueryFriend(account);
            result = statement.executeQuery(querySql);

            while(result.next()){
                userModel = new UserModel();
                userModel.setAccount(result.getString("account"));
                userModel.setNickname(result.getString("nickname"));
                userModel.setRemark(result.getString("remark"));
                userModel.setLifeMotto(result.getString("lifemotto"));
                userModel.setImageUrl(result.getString("imageurl"));
                users.add(userModel);
            }
            
            httpUserModel.setContent(users);
            
            String responseJson = gson.toJson(httpUserModel);
            out.print(responseJson);
        } catch (IOException ex) {
            if(out != null){
                httpUserModel.setResult("error");
                httpUserModel.setMessage("error: " + ex.getMessage());
                String responseJson = gson.toJson(httpUserModel);
                out.print(responseJson);
            }
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            if(out != null){
                httpUserModel.setResult("error");
                httpUserModel.setMessage("error: " + ex.getMessage());
                String responseJson = gson.toJson(httpUserModel);
                out.print(responseJson);
            }
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            DBCPUtils.closeAll(result, statement, connection);
            if(out != null){
                out.close();
            }
        }
    }
    
    private String buildSqlQuery(String account){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select * from ");
        stringBuilder.append(CommonConstant.TABLE_USER);
        stringBuilder.append(" where account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
    private String buildSqlQueryFriend(String account){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select * from ");
        stringBuilder.append(CommonConstant.TABLE_FRIEND_LIST);
        stringBuilder.append(" where foreignkey = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
    private String buildSqlUpdate(String account){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update ");
        stringBuilder.append(CommonConstant.TABLE_USER);
        stringBuilder.append(" set loginstate = 1 where account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }

}
