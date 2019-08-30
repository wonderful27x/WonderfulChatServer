/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderful;

import CommonConstant.CommonConstant;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HttpMessageModel;
import model.MessageModel;

/**
 * @Author wonderful
 * @Description 获取最新消息，这里获取的是单个朋友的消息，这里可以不上锁
 * @Date 2019-8-30
 */
public class GetNewestMessage extends HttpServlet{
        
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        HttpMessageModel httpMessageModel = new HttpMessageModel();
        PrintWriter out = null;
        String account = request.getParameter("account");
        String friendAccount = request.getParameter("friendAccount");
        Gson gson = new Gson();
        try {
            response.setContentType("text/javascript; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            out = response.getWriter();
            
            List<List<MessageModel>> messageList = new ArrayList<>();
            File file = new File(CommonConstant.MESSAGE_PATH + account + "\\" + friendAccount + ".txt");
            if(file.exists()){
                List<MessageModel> messageModel = readObject(file);
                if(messageModel.size() >0){
                    messageList.add(0,messageModel);
                }
            }
            httpMessageModel.setResult("success");
            httpMessageModel.setContent(messageList);
            String jsonData = gson.toJson(httpMessageModel);

            out.print(jsonData);
            if(file.exists()){
                deleteDir(file);
            }
        } catch (IOException ex) {
            Logger.getLogger(GetNewestMessage.class.getName()).log(Level.SEVERE, null, ex);
            if(out == null)return;
            httpMessageModel.setResult("error");
            httpMessageModel.setMessage("error: " + ex.getMessage());
            String jsonData = gson.toJson(httpMessageModel);
            out.print(jsonData);
        } finally {
            if(out != null){
                out.close();
            }
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){

    }
    
    private List<MessageModel> readObject(File file) {
        if(file == null || !file.exists()){
            return new ArrayList();
        }
    	List<MessageModel> object = null;
        ObjectInputStream inputStream = null;
        try {
            inputStream = new ObjectInputStream(new FileInputStream(file));
            object = (List<MessageModel>) inputStream.readObject();
            if(object == null){
                return new ArrayList();
            }
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(GetMessage.class.getName()).log(Level.SEVERE, null, e);
            return new ArrayList();
        }finally{
                try {
                    if(inputStream != null){
                        inputStream.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(GetMessage.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
        return object;
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
