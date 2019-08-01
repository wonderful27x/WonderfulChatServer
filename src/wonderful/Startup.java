/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderful;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
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
	ServletContext context = getServletContext();//全局Context,可以用来共享数据
        startServer();
    }

    @Override
    public void service(ServletRequest sr, ServletResponse sr1) throws ServletException, IOException {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void destroy() {
        try {
            for(SocketConnection connection:hashMap.values()){
                connection.stop();
            }
            threadPool.shutdown();
            serverSocket.close();
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
            threadPool = new ThreadPoolExecutor(0,100000,60L,TimeUnit.SECONDS,new SynchronousQueue<>(),new ThreadFactoryOfMine());
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

