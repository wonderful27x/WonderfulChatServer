/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

/**
 * @Author wonderful
 * @Description HTTP Model基础类，这是一种不怎么聪明的封装
 * @Date 2019-8-30
 */
public class HttpBaseModel<T> {
    
    private String result;
    private String message;
    private T content;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }
    
}
