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
 * @Author wonderful
 * @Description 修改密码
 * @Date 2019-8-30
 */
public class ChangePassword extends HttpServlet {

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
            String oldPass = request.getParameter("oldPass");
            String newPass = request.getParameter("newPass");
            
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();

           String querySql = buildSqlQuery(account);
           result = statement.executeQuery(querySql);

            if(result.next()){
                String password = result.getString("password");
                if(password != null && password.equals(oldPass)){
                    String update = buildSqlUpdate(account,newPass);
                    int rows = statement.executeUpdate(update);
                    if(rows > 0){
                        out.print("修改密码成功！");
                    }else{
                        out.print("修改密码失败！");
                    }
                    
                }else{
                    out.print("密码错误！");
                }
            }else{
                out.print("账号异常！");
            }

        } catch (IOException ex) {
            if(out != null){
                out.print(ex.getMessage());
            }
            Logger.getLogger(ChangePassword.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            if(out != null){
                out.print(ex.getMessage());
            }
            Logger.getLogger(ChangePassword.class.getName()).log(Level.SEVERE, null, ex);
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
    
    private String buildSqlUpdate(String account,String newPass){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update ");
        stringBuilder.append(CommonConstant.TABLE_USER);
        stringBuilder.append(" set password = '");
        stringBuilder.append(newPass);
        stringBuilder.append("' where account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
}
