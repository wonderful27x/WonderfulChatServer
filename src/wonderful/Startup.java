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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import wonderfulchat.ServletOnly;

/**
 *
 * @author Acer
 */
public class Startup extends HttpServlet{
   
    private ServerSocket serverSocket;
    private ConcurrentHashMap<String,SocketConnection> hashMap;
    private ThreadPoolExecutor threadPool;
    
    @Override
    public void init(ServletConfig sc) throws ServletException {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	//ServletContext context = getServletContext();//全局Context,可以用来共享数据
        startServer();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){
        
        PrintWriter out = null;
        response.setContentType("text/plain; charset=utf-8");
        response.setCharacterEncoding("UTF-8");
        
        try {
            out = response.getWriter();
            out.print("Have a wonderful day !");
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
    
    //初始化
    private void startServer(){
        try {
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
                while(true){
                    Socket socket = serverSocket.accept();
                    SocketConnection connection = new SocketConnection(socket,hashMap);
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

