/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderful;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import wonderfulchat.ServletOnly;

/**
 * @Author wonderful
 * @Description SERVLET自启动初始化，
 * TOMCAT启动后自动运行的servlet,
 * 作为启动项进行必要的初始化，
 * 客户端没创建一个Socket则将其封装成一个Connection并添加到Socket管理器
 * @Date 2019-8-30
 */
public class Startup extends HttpServlet{
   
    /**服务端Socket*/
    private ServerSocket serverSocket;
    /**客户端Socket管理器*/
    private ConcurrentHashMap<String,SocketConnection> hashMap;
    /**线程池*/
    private ThreadPoolExecutor threadPool;
    /**线程运行控制变量*/
    private boolean runPermit;
    /**全局context，可用于共享数据*/
    private ServletContext context;
    
    @Override
    public void init(ServletConfig sc) throws ServletException {
        super.init(sc);
        context = getServletContext();
        startServer();
    }

    /**
    * @description 通过访问接口输出一些信息，以此可以随时监测项目的运行情况
    * @param request
    * @param response
    */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){
        
        PrintWriter out = null;
        response.setContentType("text/plain; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        
        try {
            out = response.getWriter();
            out.println("Have a wonderful day !" + "\n");
            out.println("hashMap size：" + hashMap.size());
            out.println("threadPool size：" + threadPool.getPoolSize());
            out.println("thread number：" + threadPool.getActiveCount());
        } catch (IOException ex) {
            Logger.getLogger(ServletOnly.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        doPost(request,response);
    }

    @Override
    public void destroy() {
        try {
            runPermit = false;
            for(SocketConnection connection:hashMap.values()){
                connection.stop();
            }
            if(threadPool != null){
               threadPool.shutdown(); 
            }
            if(serverSocket != null){
               serverSocket.close(); 
            }
            hashMap = null;
            threadPool = null;
            serverSocket = null;
        } catch (IOException ex) {
            Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
    * @description 初始化线程池，服务端Socket等
    */
    private void startServer(){
        try {
            runPermit = true;
            threadPool = new ThreadPoolExecutor(0,200000,60L,TimeUnit.SECONDS,new SynchronousQueue<>(),new ThreadFactoryOfMine());
            hashMap = new ConcurrentHashMap<>();
            serverSocket = new ServerSocket(8888);
        } catch (IOException ex) {
            Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
        }
        ServerStart start = new ServerStart();
        start.start();        
    }
    
    class ServerStart extends Thread{
        @Override
        public void run(){
            try {
                while(runPermit){
                    Socket socket = serverSocket.accept();
                    if(!runPermit)return;
                    SocketConnection connection = new SocketConnection(context,socket,hashMap);
                    threadPool.execute(connection);
                }
            } catch (IOException ex) {
                Logger.getLogger(ServerStart.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    class ThreadFactoryOfMine implements ThreadFactory{

        private final AtomicInteger count = new AtomicInteger(1);
        
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r,"wonderfulThread #" + count.getAndIncrement());
        }
        
    }
    
}

