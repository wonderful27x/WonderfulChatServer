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
public class Logout extends HttpServlet{
    
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
            
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
//            String updateSql = "update " + CommonConstant.TABLE_USER + " set loginstate = 0 where account = '" + account + "'";
            String updateSql = buildSqlUpdate(account);
            int rows = statement.executeUpdate(updateSql);
            
            if(rows >0){
                out.print("success");
            }else{
                out.print("fail");
            }

        } catch (IOException ex) {
            out.print(ex.toString());
            Logger.getLogger(Logout.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            out.print(ex.toString());
            Logger.getLogger(Logout.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            DBCPUtils.closeAll(result, statement, connection);
            if(out != null){
                out.close();
            }
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response){

    }
    
    private String buildSqlUpdate(String account){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update ");
        stringBuilder.append(CommonConstant.TABLE_USER);
        stringBuilder.append(" set loginstate = 0 where account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
}
