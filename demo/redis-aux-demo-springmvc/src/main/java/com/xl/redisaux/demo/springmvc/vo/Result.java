package com.xl.redisaux.demo.springmvc.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result<T> {
    private T data;
    private String msg;
    private Integer code;
 
    public static Result success(Object data){
        return new Result(data,"ok",0);
    }
}