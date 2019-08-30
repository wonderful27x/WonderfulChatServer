/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderful;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author wonderful
 * @Description 获取消息，用http请求替代推送，发送简单的通知
 * @Date 2019-8-30
 */
public class GetNotice extends HttpServlet{
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        PrintWriter out = null;
        try {
            response.setContentType("text/plain; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            out = response.getWriter();
            out.print("");
        } catch (IOException ex) {
            Logger.getLogger(GetNotice.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if(out != null){
                out.close();
            }
        }
    } 
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){
        
    }

}
