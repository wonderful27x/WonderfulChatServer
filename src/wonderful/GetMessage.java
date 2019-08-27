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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HttpMessageModel;
import model.MessageModel;

/**
 *
 * @author Acer
 */
public class GetMessage extends HttpServlet{
        
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        //ServletContext context = getServletContext();//全局Context,可以用来共享数据
        //setAttribute(name,value);//往域对象里面添加数据，添加时以key-value形式添加,name是String类型，value是Object类型；
        //getAttribute(name);//根据指定的key读取域对象里面的数据
        //removeAttribute(name);//根据指定的key从域对象里面删除数据
        
        ServletContext context = getServletContext();
        HttpMessageModel httpMessageModel = new HttpMessageModel();//读写锁保证数据同步
        ReentrantReadWriteLock lock = null;
        PrintWriter out = null;
        String account = request.getParameter("account");
        Gson gson = new Gson();
        lock = (ReentrantReadWriteLock) context.getAttribute(account);
        try {
            if(lock == null){
                lock = new ReentrantReadWriteLock();
                lock.writeLock().lock();
                context.setAttribute(account,lock);
            }else{
                lock.writeLock().lock();
            }
            
            response.setContentType("text/javascript; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            out = response.getWriter();
            
            List<List<MessageModel>> messageList = new ArrayList<>();
            File file = new File(CommonConstant.MESSAGE_PATH + account);
            if(file.exists()){
                File[] files = file.listFiles();
                for(File child:files){
                    List<MessageModel> messageModel = readObject(child);
                    messageList.add(messageModel);
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
            Logger.getLogger(GetMessage.class.getName()).log(Level.SEVERE, null, ex);
            if(out == null)return;
            httpMessageModel.setResult("error");
            httpMessageModel.setMessage("error: " + ex.getMessage());
            String jsonData = gson.toJson(httpMessageModel);
            out.print(jsonData);
        } finally {
            lock.writeLock().unlock();
            context.removeAttribute(account);
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
//        if(fileDir == null || !fileDir.exists())return;
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
