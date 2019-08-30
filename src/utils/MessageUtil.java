/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import CommonConstant.MessageType;

/**
 * @Author wonderful
 * @Description 消息创建解析工具
 * @Date 2019-8-30
 */
public class MessageUtil {
    
    public static String buildUpMessage(String from,String to,Enum<MessageType> type,String message) {
            StringBuilder builder = new StringBuilder();
            builder.append("from:");
            builder.append(from);
            builder.append(" to:");
            builder.append(to);
            builder.append(" type:");
            builder.append(type);
            builder.append("$message:");
            builder.append(message);

            return builder.toString();

    }

    public static String[] getMessageHead(String message) {
            String[] messageArray;
            int index = message.indexOf("$");
            String headMessage = message.substring(0,index);
            messageArray = headMessage.split(" ");
            messageArray[0] = messageArray[0].substring(messageArray[0].indexOf(":")+1);
            messageArray[1] = messageArray[1].substring(messageArray[1].indexOf(":")+1);
            messageArray[2] = messageArray[2].substring(messageArray[2].indexOf(":")+1);

            return messageArray;

    }

    public static String getMessage(String message) {
             int index = message.indexOf("$")+1;
             String messageContent = message.substring(index);
             messageContent = messageContent.substring(8);

            return messageContent;

    }
    
}
