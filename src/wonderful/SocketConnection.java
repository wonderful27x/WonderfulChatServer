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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
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
    private boolean friendsCheck;
    private boolean initOk;
    private boolean running;
//    private String imageUrl;
    private ServletContext context;
    
    public SocketConnection(ServletContext context,Socket socket,ConcurrentHashMap<String,SocketConnection> hashMap){
        this.context = context;
        this.socket = socket;
        this.hashMap = hashMap;
        identityPass = false;
        friendsCheck = false;
        gson = new Gson();
        initOk = true;
        running = true;
        try {
            InputStream input = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            reader = new BufferedReader(new InputStreamReader(input,"UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(out,"UTF-8"));
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
                this.context = null;
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
            sendMessageToClient(MessageType.ANSWER,CommonConstant.IDENTITY_REQUEST);
            while(running && (message = reader.readLine()) != null){
                if(!running)return;
                messageModel = gson.fromJson(message, MessageModel.class);
                switch(MessageType.getByValue(messageModel.getType())){
                    case ANSWER:
                        String anserMessage = messageModel.getMessage();
                        String[] account = anserMessage.split("\\$");
                        Connection connection = DBCPUtils.getConnection();
                        Statement statement = connection.createStatement();
                        
                        String sql = buildSqlIdentity(account[0]);
                        ResultSet result = statement.executeQuery(sql);
                        if(result.next()){
                            String accountSql = result.getString("account");
                            int state = result.getInt("loginstate");
                            if(account[0].equals(accountSql) && state == 1){
//                                imageUrl = result.getString("imageurl");
                                identityPass = true;
                            }
                        }
                        
                        sql = buildSqlFriend(account[0],account[1]);
                        result = statement.executeQuery(sql);
                        if(result.next()){
                            friendsCheck = true;
                        }
                        
                        DBCPUtils.closeAll(result, statement, connection);
                        if(identityPass && friendsCheck){
                            sendMessageToClient(MessageType.ANSWER,CommonConstant.ACCEPT);
                        }else if(!identityPass){
                            sendMessageToClient(MessageType.ANSWER,CommonConstant.REFUSE);
                            return;
                        }else if(!friendsCheck){
                            sendMessageToClient(MessageType.ANSWER,CommonConstant.REFUSE_FRIEND);
                            return;
                        }
                        break;
                    case SOCKET_KEY:
                        if(!identityPass){
                            sendMessageToClient(MessageType.ANSWER,CommonConstant.REFUSE);
                            return;
                        }
                        hashMapKey = messageModel.getMessage();
                        hashMap.put(hashMapKey, this);
                        break;
                    case SOCKET_CLOSE:
                        return;
                    case MESSAGE_RECEIVE:
                        if(!identityPass){
                            sendMessageToClient(MessageType.ANSWER,CommonConstant.REFUSE);
                            return;
                        }
                        if(friendSocket == null){
                            friendSocketKey = messageModel.getReceiverAccount() + "$" + messageModel.getSenderAccount();
                            friendSocket = hashMap.get(friendSocketKey);
                        }
                        if(friendSocket == null){
                            saveMessage(messageModel);
                        }else{
                            friendSocket.sendMessageToFriend(message);
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
            sendMessageToClient(MessageType.ERROR,ex.getMessage());
        } catch (SQLException ex) {
            Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
            sendMessageToClient(MessageType.ERROR,ex.getMessage());
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
                context = null;
            } catch (IOException ex) {
                Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void saveMessage(MessageModel messageModel){
        if(hashMapKey == null)return;
        String[] key = hashMapKey.split("\\$");
        ReentrantReadWriteLock lock = (ReentrantReadWriteLock) context.getAttribute(key[0]);
        File file = createFile(CommonConstant.MESSAGE_PATH + key[1],key[0] + ".txt");
        
//        messageModel.setType(MessageType.MESSAGE_RECEIVE.getCode());
//        messageModel.setSenderImage(imageUrl);
        List<MessageModel> messageList;
        try {
            if(lock == null){
                lock = new ReentrantReadWriteLock();
                lock.readLock().lock();
                context.setAttribute(key[1],lock);
            }else{
                lock.readLock().lock();
            }

            messageList = readObject(file);
            messageList.add(messageModel);
            saveObject(file,messageList);
        }finally{
            lock.readLock().unlock();
            context.removeAttribute(key[1]);
        }
    }
    
    private List<MessageModel> readObject(File file) {
        if(file == null || !file.exists() || file.length() <=0){
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
            Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, e);
            return new ArrayList();
        }finally{
                try {
                    if(inputStream != null){
                        inputStream.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
        return object;
    }
    
    private File createFile(String directory,String name) {
            File fileDir = new File(directory);
            if(!fileDir.exists()){
                fileDir.mkdirs();
            }
            File file = new File(fileDir,name);
            if(!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            return file;
    }
    
    private void saveObject(File file,Object object) {
        ObjectOutputStream outputStream = null;
        try {
            outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(object);
        } catch (IOException e) {
            Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, e);
        }finally{
            try {
                if(outputStream != null){
                    outputStream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private String buildSqlIdentity(String account){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select * from ");
        stringBuilder.append(CommonConstant.TABLE_USER);
        stringBuilder.append(" where account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
    private String buildSqlFriend(String account,String friendAccount){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("select * from ");
        stringBuilder.append(CommonConstant.TABLE_FRIEND_LIST);
        stringBuilder.append(" where foreignkey = '");
        stringBuilder.append(friendAccount);
        stringBuilder.append(" 'and account = '");
        stringBuilder.append(account);
        stringBuilder.append("'");
        
        return stringBuilder.toString();
    }
    
    private void sendMessageToClient(MessageType type,String message){
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String time = format.format(date);
        MessageModel messageModel = buildMessage(type.getCode(),time,message,"server","server","client","client","");
        String answer = gson.toJson(messageModel);
        try {
            writer.write(answer + "\n");
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
//    private void sendMessageToFriend(MessageModel messageModel){
//        messageModel.setType(MessageType.MESSAGE_RECEIVE.getCode());
//        messageModel.setSenderImage(imageUrl);
//        String messageData = gson.toJson(messageModel);
//        try {
//            writer.write(messageData + "\n");
//            writer.flush();
//        } catch (IOException ex) {
//            Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    
    private void sendMessageToFriend(String message){
        try {
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private MessageModel buildMessage(int Type,String time,String message,String sender,String senderAccount,String receiver,String receiverAccount,String senderImage){
        
        MessageModel messageModel = new MessageModel();
        messageModel.setType(Type);
        messageModel.setTime(time);
        messageModel.setMessage(message);
        messageModel.setSender(sender);
        messageModel.setSenderAccount(senderAccount);
        messageModel.setReceiver(receiver);
        messageModel.setReceiverAccount(receiverAccount);
        messageModel.setSenderImage(senderImage);
        
        return messageModel;
    }
    
    public void resetFriendSocket(){
        friendSocket = null;
    }
    
    public void stop(){
        running = false;
        this.stop();
    }
    
}

/**
 * 这里缺少断开重连机制，客户端有可能聊天时连接中断，则会抛出异常
 */
