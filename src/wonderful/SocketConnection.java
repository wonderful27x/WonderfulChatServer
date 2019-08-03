/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderful;

import CommonConstant.CommonConstant;
import CommonConstant.MessageType;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.MessageModel;
import utils.DBCPUtils;

/**
 *
 * @author Acer
 */
public class SocketConnection implements Runnable{
    
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String hashMapKey;
    private ConcurrentHashMap<String,SocketConnection> hashMap;
    private SocketConnection friendSocket;
    private String friendSocketKey;
    private Gson gson;
    private boolean identityPass;
    private boolean initOk;
    private boolean running;
    private String imageUrl;
    
    public SocketConnection(Socket socket,ConcurrentHashMap<String,SocketConnection> hashMap){
        this.socket = socket;
        this.hashMap = hashMap;
        identityPass = false;
        gson = new Gson();
        initOk = true;
        running = true;
        try {
            InputStream input = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            reader = new BufferedReader(new InputStreamReader(input));
            writer = new BufferedWriter(new OutputStreamWriter(out));
        } catch (IOException ex) {
            Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
            initOk = false;
            try {
                if(socket != null){
                    socket.close();
                }
                if(reader != null){
                    reader.close();
                }
                if(writer != null){
                    writer.close();
                }
                this.hashMap = null;
            } catch (IOException ex1) {
                Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    @Override
    public void run() {
        if(!initOk)return;
        String message;
        MessageModel messageModel;
        try {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            sendMessage(MessageType.ANSWER,CommonConstant.IDENTITY_REQUEST);
            while(running && (message = reader.readLine()) != null){
                if(!running)return;
                messageModel = gson.fromJson(message, MessageModel.class);
                switch(MessageType.getByValue(messageModel.getType())){
                    case ANSWER:
                        String account = messageModel.getMessage();
                        Connection connection = DBCPUtils.getConnection();
                        Statement statement = connection.createStatement();
                        String sql = buildSql(account);
                        ResultSet result = statement.executeQuery(sql);
                        if(result.next()){
                            String accountSql = result.getString("account");
                            int state = result.getInt("loginstate");
                            if(account.equals(accountSql) && state == 1){
                                imageUrl = result.getString("imageurl");
                                identityPass = true;
                            }
                        }
                        DBCPUtils.closeAll(result, statement, connection);
                        if(identityPass){
                            sendMessage(MessageType.ANSWER,CommonConstant.ACCEPT);
                        }else{
                            sendMessage(MessageType.ANSWER,CommonConstant.REFUSE);
                            return;
                        }
                        break;
                    case SOCKET_KEY:
                        if(!identityPass){
                            sendMessage(MessageType.ANSWER,CommonConstant.REFUSE);
                            return;
                        }
                        hashMapKey = messageModel.getMessage();
                        hashMap.put(hashMapKey, this);
                        break;
                    case SOCKET_CLOSE:
                        return;
                    case MESSAGE_SEND:
                        if(!identityPass){
                            sendMessage(MessageType.ANSWER,CommonConstant.REFUSE);
                            return;
                        }
                        if(friendSocket == null){
                            friendSocketKey = messageModel.getReceiverAccount() + "$" + messageModel.getSenderAccount();
                            friendSocket = hashMap.get(friendSocketKey);
                        }
                        if(friendSocket == null){
                            saveMessage();
                        }else{
                            friendSocket.sendMessage(MessageType.MESSAGE_RECEIVE,messageModel.getSenderAccount(),messageModel.getReceiverAccount(),messageModel.getMessage());
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
            sendMessage(MessageType.ERROR,ex.getMessage());
        } catch (SQLException ex) {
            Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
            sendMessage(MessageType.ERROR,ex.getMessage());
        }finally{
            try {
                SocketConnection connection = null;
                if(friendSocketKey != null && !friendSocketKey.isEmpty()){
                    connection = hashMap.get(friendSocketKey);
                }
                if(connection != null){
                    connection.resetFriendSocket();
                }
                
                if(socket != null){
                    socket.close();
                }
                if(reader != null){
                    reader.close();
                }
                if(writer != null){
                    writer.close();
                }
                if(hashMap != null){
                   hashMap.remove(hashMapKey); 
                }
                hashMap = null;
                friendSocket = null;
            } catch (IOException ex) {
                Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
//    public void sendMessage(String message){
//        try {
//            writer.write(message + "\n");
//        } catch (IOException ex) {
//            Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    
    private void saveMessage(){
        
    }
    
    private String buildSql(String account){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select * from ");
        stringBuilder.append(CommonConstant.TABLE_USER);
        stringBuilder.append(" where account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
    private void sendMessage(MessageType type,String message){
            MessageModel messageModel = new MessageModel();
            messageModel.setType(type.getCode());
            messageModel.setSenderAccount("server");
            messageModel.setReceiverAccount("client");
            messageModel.setSenderImage(imageUrl);
            messageModel.setMessage(message);
            String answer = gson.toJson(messageModel);
        try {
            writer.write(answer + "\n");
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void sendMessage(MessageType type,String sender,String receiver,String message){
            MessageModel messageModel = new MessageModel();
            messageModel.setType(type.getCode());
            messageModel.setSenderAccount(sender);
            messageModel.setReceiverAccount(receiver);
            messageModel.setSenderImage(imageUrl);
            messageModel.setMessage(message);
            String messageData = gson.toJson(messageModel);
        try {
            writer.write(messageData + "\n");
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void resetFriendSocket(){
        friendSocket = null;
    }
    
    public void stop(){
        running = false;
        this.stop();
    }
    
}
