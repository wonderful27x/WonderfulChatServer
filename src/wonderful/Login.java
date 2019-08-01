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
import wonderful.Register;
import utils.DBCPUtils;

/**
 *
 * @author Acer
 */
public class Login extends HttpServlet {

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

    public void doPost(HttpServletRequest request, HttpServletResponse response){
        PrintWriter out = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        
        HttpUserModel httpUserModel = new HttpUserModel();
        List<UserModel> users = new ArrayList();
        UserModel userModel = new UserModel();
        
        try {
            response.setContentType("text/javascript; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            out = response.getWriter();
            String account = request.getParameter("account");
            String pass = request.getParameter("password");
            
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
//            String querySql = "select * from " + CommonConstant.TABLE_USER + " where account = '" + account + "'";
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
                    httpUserModel.setContent(users);
                    httpUserModel.setResult("success");
                    
//                    String update = "update " + CommonConstant.TABLE_USER + " set loginstate = 1 where account = '" + account + "'";
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
            Gson gson = new Gson();
            String responseJson = gson.toJson(httpUserModel);
            out.print(responseJson);
        } catch (IOException ex) {
            out.print(ex.toString());
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            out.print(ex.toString());
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
