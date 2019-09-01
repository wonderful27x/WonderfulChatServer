/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import CommonConstant.CommonConstant;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.dbcp2.BasicDataSource;

/**
 * @Author wonderful
 * @Description MYSQL连接池
 * @Date 2019-8-30
 */
public class DBCPUtils {
    /**
    * 声明DBCP
    */
    private static final BasicDataSource dataSource = new BasicDataSource();
    
    static {
//        String url = "jdbc:mysql://localhost:3306/" + CommonConstant.DATABASE + "?useSSL=false&serverTimezone=UTC";
//        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/" + CommonConstant.DATABASE;
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername("root");
        dataSource.setPassword("");
        
        dataSource.setInitialSize(5);
        dataSource.setMaxTotal(450);          //#1 最大并发数，在同一时刻最多可以创建的连接数
        dataSource.setMaxIdle(20);            //#2 最大空闲连接，超出则将被回收，这意味着连接池中不可能有比此数量更多的空闲连接
        dataSource.setMinIdle(5);             //#3 最小空闲连接，连接池中始终保持的空闲连接
        dataSource.setMaxWaitMillis(5000);    //#4 在#1达到最大时，将无法获得空闲连接，当时间超出此时间仍然没有获取到空闲连接时将抛出异常
        
        dataSource.setTestOnBorrow(true);
        dataSource.setTestOnReturn(false);
        
        dataSource.setValidationQuery("select 1");
        dataSource.setValidationQueryTimeout(1);
        
        /**
        * 空闲连接回收机制，注意这里的回收是指被close返回到连接池中的空闲连接,
        * 根据#2、#3我们知道，这个回收范围是#2-#3设定值的范围，
        * 因为这是“空闲”回收，而连接池中最大的空闲连接就是MaxIdle
        */
        dataSource.setTimeBetweenEvictionRunsMillis(1000*60*10);//回收空闲连接的时间间隔
        dataSource.setNumTestsPerEvictionRun(10);               //每次检测回收的数量
        dataSource.setMinEvictableIdleTimeMillis(1000*60*8);    //空闲连接超过次时间将被回收
        
        dataSource.setDefaultAutoCommit(true);
        
        /**
        * 连接池以外被创建但并未close的回收
        */
        dataSource.setRemoveAbandonedOnBorrow(true);
        dataSource.setRemoveAbandonedOnMaintenance(true);
        dataSource.setRemoveAbandonedTimeout(300);               //回收时间间隔，单位s,经实测，如果有大量没有close的连接，超过此时间将被迅速回收，最后剩下的连接等于MinIdle
        
    }
    
    /**
     * @throws java.sql.SQLException
    * @description 从池中获取一个连接
    * @return Connection
    */
    public static Connection getConnection() throws SQLException{
        return dataSource.getConnection();
    }
    
    public static void closeAll(ResultSet rs,Statement stmt,Connection conn){
        if(rs!=null){
            try {
                rs.close();
            } catch (SQLException e) {
                Logger.getLogger(DBCPUtils.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        
        if(stmt!=null){
            try {
                stmt.close();
            } catch (SQLException e) {
                Logger.getLogger(DBCPUtils.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        
        if(conn!=null){
            try {
                conn.close();
            } catch (SQLException e) {
                Logger.getLogger(DBCPUtils.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }
}
