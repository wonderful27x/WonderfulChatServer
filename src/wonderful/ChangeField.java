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

public class ChangeField extends HttpServlet{
    
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
            String field = request.getParameter("field");
            String content = request.getParameter("content");
            
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
//            String updateSql = "update " + CommonConstant.TABLE_USER + " set " + field + " = '" + content + "'" + " where account = '" + account + "'";
            String updateSql = buildSql(account,field,content);
            int rows = statement.executeUpdate(updateSql);
            
            if(rows >0){
                out.print("success");
            }else{
                out.print("fail");
            }
            
            String buildSqlSynchronize = buildSqlSynchronize(account,field,content);
            int rowsInSynchronize = statement.executeUpdate(buildSqlSynchronize);
            
           Logger.getLogger(ChangeField.class.getName()).log(Level.SEVERE, null, rowsInSynchronize);
            

        } catch (IOException ex) {
            out.print(ex.toString());
            Logger.getLogger(ChangeField.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            out.print(ex.toString());
            Logger.getLogger(ChangeField.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            DBCPUtils.closeAll(result, statement, connection);
            if(out != null){
                out.close();
            }
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response){

    }
    
    private String buildSql(String account,String field,String content){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update ");
        stringBuilder.append(CommonConstant.TABLE_USER);
        stringBuilder.append(" set ");
        stringBuilder.append(field);
        stringBuilder.append(" = '");
        stringBuilder.append(content);
        stringBuilder.append("' where account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
    private String buildSqlSynchronize(String account,String field,String content){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update ");
        stringBuilder.append(CommonConstant.TABLE_FRIEND_LIST);
        stringBuilder.append(" set ");
        stringBuilder.append(field);
        stringBuilder.append(" = '");
        stringBuilder.append(content);
        stringBuilder.append("' where account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
}
