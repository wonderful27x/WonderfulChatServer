/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wonderfulchat;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Acer
 */
public class WonderfulChat {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        AtomicInteger count = new AtomicInteger(1);
        System.out.println(count.getAndIncrement());
        System.out.println(count.getAndIncrement());
    }
    
}
