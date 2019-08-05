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
public class FindFriend extends HttpServlet{
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
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
            
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
//            String querySql = "select * from " + CommonConstant.TABLE_USER + " where account = '" + account + "'";
            String querySql = buildSql(account);
            result = statement.executeQuery(querySql);

            if(result.next()){
                userModel.setAccount(result.getString("account"));
                userModel.setNickname(result.getString("nickname"));
                userModel.setLifeMotto(result.getString("lifemotto"));
                userModel.setImageUrl(result.getString("imageurl"));
                users.add(userModel);
            }
            
            httpUserModel.setContent(users);
            httpUserModel.setResult("success");
            
            String responseJson = gson.toJson(httpUserModel);
            out.print(responseJson);
        } catch (IOException ex) {
            if(out != null){
                httpUserModel.setResult("error");
                httpUserModel.setMessage("error: " + ex.getMessage());
                String responseJson = gson.toJson(httpUserModel);
                out.print(responseJson);
            }
            Logger.getLogger(FindFriend.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            if(out != null){
                httpUserModel.setResult("error");
                httpUserModel.setMessage("error: " + ex.getMessage());
                String responseJson = gson.toJson(httpUserModel);
                out.print(responseJson);
            }
            Logger.getLogger(FindFriend.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            DBCPUtils.closeAll(result, statement, connection);
            if(out != null){
                out.close();
            }
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){

    }
    
    private String buildSql(String account){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select * from ");
        stringBuilder.append(CommonConstant.TABLE_USER);
        stringBuilder.append(" where account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
}
