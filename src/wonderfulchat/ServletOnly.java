package wonderfulchat;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Acer
 */
public class ServletOnly implements Servlet {

    private ServerSocket service = null;
    private Socket socket = null;

    @Override
    public void init(ServletConfig sc) throws ServletException {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	startServer();
    }

    @Override
    public ServletConfig getServletConfig() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return null;
    }

    @Override
    public void service(ServletRequest sr, ServletResponse sr1) throws ServletException, IOException {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getServletInfo() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return null;
    }

    @Override
    public void destroy() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    public void startServer(){
	new Thread(new Runnable(){
	    @Override
            public void run() {
		try {
            	     service = new ServerSocket(8888);
            	     while(true){
                	socket = service.accept();
                	managerSocket(socket);
            	     }
        	} catch (IOException ex) {
            	     //Logger.getLogger(SocketChatServer.class.getName()).log(Level.SEVERE, null, ex);
        	}finally{
            	    try {
                	if(service!=null){
                           service.close();
                	}
                	if(socket!=null){
                           socket.close();
                	}
            	     } catch (IOException ex) {
                        //Logger.getLogger(SocketChatServer.class.getName()).log(Level.SEVERE, null, ex);
            	     }
        	}
	    }
	}).start();
    }
    
    public void managerSocket(Socket socket){
        //System.out.println("Client: "+socket.hashCode()+" ����...");
        new Thread(new Runnable(){
                @Override
            public void run() {
                try {
                    InputStream input = socket.getInputStream();
                    OutputStream out = socket.getOutputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		    writer.write("server is ok" + "\n");
                    writer.flush();
                    String readSomething;
                    while((readSomething = reader.readLine()) != null){
                        writer.write(readSomething + "\n");
                        writer.flush();
                        System.out.println(readSomething);
                    }
                } catch (IOException ex) {
                    //Logger.getLogger(SocketChatServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
    }
    
}