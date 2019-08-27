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
import model.FriendRequestModel;
import model.HttpFriendRequest;
import utils.DBCPUtils;

/**
 *
 * @author Acer
 */
public class GetFriendRequest extends HttpServlet{
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        PrintWriter out = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        
        HttpFriendRequest httpFriendRequest = new HttpFriendRequest();
        List<FriendRequestModel> requestList = new ArrayList();
        FriendRequestModel requestModel;
        Gson gson = new Gson();
        try {
            response.setContentType("text/javascript; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            out = response.getWriter();
            String account = request.getParameter("account");
            
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
            
            String querySql = buildSql(account);
            result = statement.executeQuery(querySql);

            while(result.next()){
                requestModel = new FriendRequestModel();
                requestModel.setAccount(result.getString("account"));
                requestModel.setNickname(result.getString("nickname"));
                requestModel.setLifeMotto(result.getString("lifemotto"));
                requestModel.setImageUrl(result.getString("imageurl"));
                requestModel.setRequestTime(result.getString("requesttime"));
                requestList.add(requestModel);
            }
            
            httpFriendRequest.setContent(requestList);
            httpFriendRequest.setResult("success");
            
            String responseJson = gson.toJson(httpFriendRequest);
            out.print(responseJson);
        } catch (IOException ex) {
            if(out != null){
                httpFriendRequest.setResult("error");
                httpFriendRequest.setMessage("error: " + ex.getMessage());
                String responseJson = gson.toJson(httpFriendRequest);
                out.print(responseJson);
            }
            Logger.getLogger(GetFriendRequest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            if(out != null){
                httpFriendRequest.setResult("error");
                httpFriendRequest.setMessage("error: " + ex.getMessage());
                String responseJson = gson.toJson(httpFriendRequest);
                out.print(responseJson);
            }
            Logger.getLogger(GetFriendRequest.class.getName()).log(Level.SEVERE, null, ex);
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
    
    private String buildSql(String host){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select * from ");
        stringBuilder.append(CommonConstant.TABLE_FRIEND_REQUEST);
        stringBuilder.append(" where host = '");
        stringBuilder.append(host);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
}
