/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderful;

import CommonConstant.CommonConstant;
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
 *
 * @author Acer
 */
public class ChangeRemark extends HttpServlet{
    
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
            String content = request.getParameter("content");
            
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
//            String updateSql = "update " + CommonConstant.TABLE_USER + " set " + field + " = '" + content + "'" + " where account = '" + account + "'";
            String updateSql = buildSql(account,friendAccount,content);
            int rows = statement.executeUpdate(updateSql);
            
            if(rows >0){
                out.print("success");
            }else{
                out.print("fail");
            }

        } catch (IOException ex) {
            if(out != null){
                out.print(ex.getMessage());
            }
            Logger.getLogger(ChangeRemark.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            if(out != null){
                out.print(ex.getMessage());
            }
            Logger.getLogger(ChangeRemark.class.getName()).log(Level.SEVERE, null, ex);
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
    
    private String buildSql(String account,String friendAccount,String content){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update ");
        stringBuilder.append(CommonConstant.TABLE_FRIEND_LIST);
        stringBuilder.append(" set remark = '");
        stringBuilder.append(content);
        stringBuilder.append("' where foreignkey = '");
        stringBuilder.append(account);
        stringBuilder.append("' and account = '");
        stringBuilder.append(friendAccount);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
}
