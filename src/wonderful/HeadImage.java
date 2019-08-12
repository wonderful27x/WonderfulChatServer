/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderful;

import CommonConstant.CommonConstant;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import java.io.File;
import java.io.FileOutputStream;
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
public class HeadImage extends HttpServlet {
    
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
        Connection connection = null;
        Statement statement = null;
        ResultSet result = null;
        PrintWriter out = null;
        FileOutputStream outputStream = null;
        
        try {
            response.setContentType("text/plain; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            out = response.getWriter();
            String account = request.getParameter("account");
            String imageName = request.getParameter("imageName");
            String imageString = request.getParameter("imageString");
            
            File imageFile = new File(CommonConstant.IMAGE_PATH + imageName);
            if(imageFile.exists() && imageFile.isFile()){
                imageFile.delete();
            }
                 
            long time = System.currentTimeMillis();
            String imageUrl = CommonConstant.HOST + "image/" + account + "$" + time + ".jpg";
            String imagePath = CommonConstant.IMAGE_PATH + account + "$" + time + ".jpg";
            byte[] imageByteArray = Base64.decode(imageString);
            outputStream = new FileOutputStream(imagePath);
            outputStream.write(imageByteArray);
            outputStream.close();
            
            connection = DBCPUtils.getConnection();
            statement = connection.createStatement();
            String updateSql = buildSqlUpdate(account,CommonConstant.TABLE_USER,imageUrl);
            int rows = statement.executeUpdate(updateSql);
            if(rows<0){
                imageFile = new File(imagePath);
                if(imageFile.exists() && imageFile.isFile()){
                    imageFile.delete();
                }
                out.print("fail");
                return;
            }
            
            String querySql = buildSqlQuery(account,CommonConstant.TABLE_FRIEND_LIST);
            result = statement.executeQuery(querySql);
            if(!result.next()){
                out.print("success$" + imageUrl);
                return;
            }
            
            updateSql = buildSqlUpdate(account,CommonConstant.TABLE_FRIEND_LIST,imageUrl);
            rows = statement.executeUpdate(updateSql);
            if(rows>0){
                out.print("success$" + imageUrl);
            }else{
                imageFile = new File(imagePath);
                if(imageFile.exists() && imageFile.isFile()){
                    imageFile.delete();
                }
                out.print("fail");
            }
                    
        } catch (IOException ex) {
            if(out != null){
                out.print(ex.getMessage());
            }
            Logger.getLogger(HeadImage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Base64DecodingException ex) {
            if(out != null){
                out.print(ex.getMessage());
            }
            Logger.getLogger(HeadImage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            if(out != null){
                out.print(ex.getMessage());
            }
            Logger.getLogger(HeadImage.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            if(out != null){
                out.close();
            }
            DBCPUtils.closeAll(result, statement, connection);
            try {
                outputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(HeadImage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private String buildSqlUpdate(String account,String table,String imageUrl){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update ");
        stringBuilder.append(table);
        stringBuilder.append(" set imageurl = '");
        stringBuilder.append(imageUrl);
        stringBuilder.append("' where account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
    private String buildSqlQuery(String account,String table){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select * from ");
        stringBuilder.append(table);
        stringBuilder.append(" where account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
            
}


//public class HeadImage extends HttpServlet {
//    
//    @Override
//    public void doGet(HttpServletRequest request, HttpServletResponse response)
//    throws IOException, ServletException
//    {
//        response.setContentType("text/plain; charset=utf-8");
//	response.setCharacterEncoding("UTF-8");
//        PrintWriter out = response.getWriter();
//	String string = "Have a wonderful day";
//	out.print(string);
//	out.close();
//
//    }
//    
//    @Override
//    public void doPost(HttpServletRequest request, HttpServletResponse response){
//        Connection connection = null;
//        Statement statement = null;
//        ResultSet result = null;
//        PrintWriter out = null;
//        FileOutputStream outputStream = null;
//        
//        try {
//            response.setContentType("text/plain; charset=utf-8");
//            response.setCharacterEncoding("UTF-8");
//            out = response.getWriter();
//            String account = request.getParameter("account");
//            String imageString = request.getParameter("imageString");
//            String imageUrlLastState = request.getParameter("imageUrlLastState");
//            File imageFile;
//            
//            if(imageUrlLastState.equals("$0")){
//                imageFile = new File(CommonConstant.IMAGE_PATH + account + imageUrlLastState + ".jpg");
//                imageUrlLastState = "$1";
//            }else{
//                imageFile = new File(CommonConstant.IMAGE_PATH + account + imageUrlLastState + ".jpg");
//                imageUrlLastState = "$0";
//            }
//            if(imageFile.exists() && imageFile.isFile()){
//                imageFile.delete();
//            }
//                   
//            String imageUrl = CommonConstant.HOST + "image/" + account + imageUrlLastState + ".jpg";
//            byte[] imageByteArray = Base64.decode(imageString);
//            outputStream = new FileOutputStream(CommonConstant.IMAGE_PATH + account + imageUrlLastState + ".jpg");
//            outputStream.write(imageByteArray);
//            
//            connection = DBCPUtils.getConnection();
//            statement = connection.createStatement();
//            String updateSql = buildSqlUpdate(account,CommonConstant.TABLE_USER,imageUrl);
//            int rows = statement.executeUpdate(updateSql);
//            if(rows<0){
//                out.print("fail");
//                return;
//            }
//            updateSql = buildSqlUpdate(account,CommonConstant.TABLE_FRIEND_LIST,imageUrl);
//            rows = statement.executeUpdate(updateSql);
//            if(rows>0){
//                out.print("success$" + imageUrl);
//            }else{
//                out.print("fail");
//            }
//                    
//        } catch (IOException ex) {
//            if(out != null){
//                out.print(ex.getMessage());
//            }
//            Logger.getLogger(HeadImage.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (Base64DecodingException ex) {
//            if(out != null){
//                out.print(ex.getMessage());
//            }
//            Logger.getLogger(HeadImage.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (SQLException ex) {
//            if(out != null){
//                out.print(ex.getMessage());
//            }
//            Logger.getLogger(HeadImage.class.getName()).log(Level.SEVERE, null, ex);
//        }finally{
//            if(out != null){
//                out.close();
//            }
//            DBCPUtils.closeAll(result, statement, connection);
//            try {
//                outputStream.close();
//            } catch (IOException ex) {
//                Logger.getLogger(HeadImage.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
//    
//    private String buildSqlUpdate(String account,String table,String imageUrl){
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append("update ");
//        stringBuilder.append(table);
//        stringBuilder.append(" set imageurl = '");
//        stringBuilder.append(imageUrl);
//        stringBuilder.append("' where account = '");
//        stringBuilder.append(account);
//        stringBuilder.append("'");
//        
//        return stringBuilder.toString();
//    }
//            
//}
