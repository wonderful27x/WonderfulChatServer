/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderful;

import CommonConstant.CommonConstant;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import utils.DBCPUtils;

/**
 * @Author wonderful
 * @Description 删除朋友
 * @Date 2019-8-30
 */
public class DeleteFriend extends HttpServlet{
    
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

            String updateSql = buildSql(account,friendAccount);
            int rows = statement.executeUpdate(updateSql);
            
            if(rows >0){
                out.print("success");
            }else{
                out.print("fail");
            }
            
            /**同时删除好友消息*/
            File file = new File(CommonConstant.MESSAGE_PATH + account + "\\" + friendAccount + ".txt");
            if(file.exists()){
                deleteDir(file);
            }

        } catch (IOException ex) {
            if(out != null){
                out.print(ex.getMessage());
            }
            Logger.getLogger(DeleteFriend.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            if(out != null){
                out.print(ex.getMessage());
            }
            Logger.getLogger(DeleteFriend.class.getName()).log(Level.SEVERE, null, ex);
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
    
    private String buildSql(String account,String friendAccount){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("delete from ");
        stringBuilder.append(CommonConstant.TABLE_FRIEND_LIST);
        stringBuilder.append(" where foreignkey = '");
        stringBuilder.append(account);
        stringBuilder.append("' and account = '");
        stringBuilder.append(friendAccount);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
    private void deleteDir(File fileDir) {
    	File[] files = fileDir.listFiles();
    	if(files != null) {
    		for(File file : files) {
    			if(file.isDirectory()) {
    				deleteDir(file);
    			}else if(file.isFile()) {
    				file.delete();
    			}
    		}
    	}
    	fileDir.delete();
    }
    
}
