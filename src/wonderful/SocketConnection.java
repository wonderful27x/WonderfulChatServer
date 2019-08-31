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
 * @Author wonderful
 * @Description SOCKET核心封装类，
 * 将socket封装成一个类，让其拥有身份验证，消息转发，消息存储等功能
 * SocketConnection持有Socket管理器HashMap，当身份验证通过时将自己添加到管理器中
 * @Date 2019-8-30
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
    private ServletContext context;
    
    /**
    * @description 构造函数，数据初始化
    * @param context
    * @param socket
    * @param hashMap
    */
    public SocketConnection(ServletContext context,Socket socket,ConcurrentHashMap<String,SocketConnection> hashMap){
        this.context = context;
        this.socket = socket;
        this.hashMap = hashMap;
        identityPass = false;
        friendsCheck = false;
        gson = new Gson();
        initOk = true;
        running = true;
        hashMapKey = "";
        friendSocketKey = "";
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

    /**
     * 特别注意：里面的使用了带while循环的阻塞语句，
     * 在switch中break和return具有特殊的用途，不能互换，否则会出现微妙的bug
     */
    @Override
    public void run() {
        if(!initOk)return;
        String message;
        MessageModel messageModel;
        try {
            /** 
             * 阶段一：
             * socket创建后向client发起身份验证请求
             */
            sendMessageToClient(MessageType.ANSWER,CommonConstant.IDENTITY_REQUEST);        
            while(running && (message = reader.readLine()) != null){
                if(!running)return;
                messageModel = gson.fromJson(message, MessageModel.class);
                switch(MessageType.getByValue(messageModel.getType())){
                    /** 
                     * 阶段二：
                     * 拿到应答后首先验证账号登录状态，再验证好友是否也添加了自己，
                     * 如果验证失败则禁止消息转发和存储
                     */
                    case ANSWER:
                        String anserMessage = messageModel.getMessage();
                        if(anserMessage == null || anserMessage.split("\\$").length != 2){
                            sendMessageToClient(MessageType.ANSWER,CommonConstant.REFUSE);
                            return;
                        }
                        String[] account = anserMessage.split("\\$");
                        Connection connection = DBCPUtils.getConnection();
                        Statement statement = connection.createStatement();
                        
                        //验证账号是否为登录状态
                        String sql = buildSqlIdentity(account[0]);
                        ResultSet result = statement.executeQuery(sql);
                        if(result.next()){
                            String accountSql = result.getString("account");
                            int state = result.getInt("loginstate");
                            if(account[0].equals(accountSql) && state == 1){
                                identityPass = true;
                            }
                        }
                        
                        //验证对方是否已添加自己为好友
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
                    /** 
                     * 阶段三：
                     * 验证通过后client向server端发送socket key,server将此socket加入管理器，同时将这个key转化为朋友的socket key,
                     * 这里的逻辑是每个client建立的socket是指向朋友对自己的socket的,这就明确了通信的方向，即A要跟B聊天，A建立了一个指向B的socket,
                     * 哈希键为A.account$B.account,而B也必须建立指向A的socket,哈希键为B.account$A.account,双方才能通信
                     * 理论上来说这个key应该是由服务端指定的，这样做只是为了获得client的应答，实际上client是不能指定自己的socket key的
                     * 注意：这里说的socket key 指的就是管理器的哈希键
                     */
                    case SOCKET_KEY:
                        if(!identityPass){
                            sendMessageToClient(MessageType.ANSWER,CommonConstant.REFUSE);
                            return;
                        }
                        if(messageModel.getMessage() == null)return;
                        hashMapKey = messageModel.getMessage();
                        hashMap.put(hashMapKey, this);
                        friendSocketKey = changeToFriendSocketKey(hashMapKey);
                        break;
                    /** 
                     * 阶段四：
                     * 验证都通过后双方开始互发消息，服务器只是一个中转站，
                     * 注意：这时候有可能发生两种情况，一是双方都建立了指向对方的socket,这时服务器直接转发消息，
                     * 二是A建立了指向B的socket,但是B没有建立socket或指向的不是A，这时则认为B不在线，
                     * 则将消息存储到文件系统，当B请求数据时将消息取出返回，这里判断B是否在线的依据就是以键值为
                     * B.account$A.account从管理器中取出socket,如果存在说明在线，否则不在线
                     */
                    case MESSAGE_RECEIVE:
                        if(!identityPass){
                            sendMessageToClient(MessageType.ANSWER,CommonConstant.REFUSE);
                            return;
                        }
                        if(friendSocket == null){
                            friendSocket = hashMap.get(friendSocketKey);
                        }
                        if(friendSocket == null){
                            saveMessage(messageModel);
                        }else{
                            friendSocket.sendMessageToFriend(message);
                        }
                        break;
                    /** 
                     * 阶段五：
                     * client离开时主动请求关闭socket,server调用close,在阻塞的情况下，只有这种方法是安全的，
                     * 但确是不可靠的，这里并没有真正的关闭socket，只是break,将最终的资源释放交给finally处理,
                     * 注意：这里必须使用return,不能使用break!
                     */
                    case SOCKET_CLOSE:
                        //Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, "Bye!", "null");
                        return;
                    default:
                        return;
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
                //Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, "Finally!", "Finally!");
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
                if(hashMap != null && hashMapKey != null){
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
    
   /**
    * @description 当好友不在线时，将数据存入文件系统后缀名为account.txt
    * A发给B的消息：将消息存入path + B.account的文件夹中，并命名为A.account.txt
    * 当B请求数据后将整个文件夹删除，这里出现并发时的安全隐患而引入锁机制，又因为需要锁住整个文件夹，因此使用了读写锁，
    * 当B读取消息时获取的是写锁，A写消息时却是读锁
    * @param messageModel
    */
    private void saveMessage(MessageModel messageModel){
        if(hashMapKey == null || hashMapKey.split("\\$").length != 2)return;
        String[] key = hashMapKey.split("\\$");
        /**
         * 读写锁保证数据同步，这里上锁的为整个文件夹，所以文件锁无法实现
         * 注意：因为读写数据并不是在同一个Servlet,因此通过全局context来共享数据，以hashMap存储，锁的键值为B.account
         */
        ReentrantReadWriteLock lock = (ReentrantReadWriteLock) context.getAttribute(key[1]); 
        File file = createFile(CommonConstant.MESSAGE_PATH + key[1],key[0] + ".txt");     
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
            /**
             * 这里对锁的控制仍然不理想，假设A和C同时读锁，A于C先读完，于是A释放了锁，
             * 但是C仍然在读，这时B有可能开始写文件，造成数据不同步
             */
            lock.readLock().unlock();
            context.removeAttribute(key[1]);
        }
    }
    
    private List<MessageModel> readObject(File file) {
        if(file == null || !file.exists() || file.length() <= 0){
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
    
    /**
    * @description 将自己的socketKey转换成朋友的socketKey
    * @param socketKey
    * @return String
    */
    private String changeToFriendSocketKey(String socketKey){
        String friendKey = "";
        if(socketKey != null){
            String[] keyArray = socketKey.split("\\$");
            if(keyArray.length == 2){
                friendKey = keyArray[1] + "$" + keyArray[0];
            }
        }
               
        return friendKey;
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
    
    /**
    * @description server向client发送消息
    * @param type
    * @param message
    */
    private void sendMessageToClient(MessageType type,String message){
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:HH:ss");
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
    
    /**
    * @description 如果朋友在线则直接将数据转发
    * @param message
    */
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
        //Logger.getLogger(SocketConnection.class.getName()).log(Level.SEVERE, "My friendSocket has been reset!", "My friendSocket has been reset!");
    }
    
    public void stop(){
        running = false;
        this.stop();
    }
    
}

/**
 * 这里缺少断开重连机制，客户端有可能聊天时连接中断，则会抛出异常
 */
