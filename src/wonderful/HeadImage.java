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
 * @Author wonderful
 * @Description 头像上传，采用base64传输，
 * 头像修改成功后同时修改其每一个好友中自己对应的头像，即friendlist表中所有账号为account(修改者账号)的数据
 * @Date 2019-8-30
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
                 
            /**
             * 这里采用账号+时间戳的方式命名，保证url的唯一性，
             * 同时保证每次修改头像后url都变化
             * 这样做是为了client中使用glide加载图片也能及时更新
             */
            long time = System.currentTimeMillis();
            String imageUrl = CommonConstant.HOST + "image/" + account + "$" + time + ".jpg";
            String imagePath = CommonConstant.IMAGE_PATH + account + "$" + time + ".jpg";
            byte[] imageByteArray = Base64.decode(imageString);
            outputStream = new FileOutputStream(imagePath);
            outputStream.write(imageByteArray);
            outputStream.close();
            
            /**
             * 修改用户表中头像url
             */
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
            
            /**
             * 先判断friendlist中是否有account数据，如果没有说明自己不是任何人的好友，直接返回成功
             */
            String querySql = buildSqlQuery(account,CommonConstant.TABLE_FRIEND_LIST);
            result = statement.executeQuery(querySql);
            if(!result.next()){
                out.print("success$" + imageUrl);
                return;
            }
            
            /**
             * 如果friendlist中有account数据，则更新所有account头像url
             */
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
