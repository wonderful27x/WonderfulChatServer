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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import utils.DBCPUtils;

/**
 *
 * @author Acer
 */
public class Register extends HttpServlet{
       
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {
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
        
        try {
            response.setContentType("text/plain; charset=utf-8");
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
                out.print("账号已存在！");
            }else{
//                String querySql = "insert into " + CommonConstant.TABLE_USER + "(account,password) values('" + account + "','" + pass + "')";
                String updateSql = buildSqlUpdate(account,pass);
                int rows = statement.executeUpdate(updateSql);
                if(rows>0){
                    out.print("注册成功！");
                }else{
                    out.print("注册失败！");
                }
            }
            
        } catch (IOException ex) {
            if(out != null){
                out.print(ex.getMessage());
            }
            Logger.getLogger(Register.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            if(out != null){
                out.print(ex.getMessage());
            }
            Logger.getLogger(Register.class.getName()).log(Level.SEVERE, null, ex);
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
    
    private String buildSqlUpdate(String account,String pass){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("insert into ");
        stringBuilder.append(CommonConstant.TABLE_USER);
        stringBuilder.append(" (account,password) values('");
        stringBuilder.append(account);
        stringBuilder.append("','");
        stringBuilder.append(pass);
        stringBuilder.append("')");
        
        return stringBuilder.toString();
    }
}
