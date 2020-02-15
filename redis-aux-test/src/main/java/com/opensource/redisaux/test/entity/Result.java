package com.opensource.redisaux.test.entity;

import lombok.Data;

/**
 * @author lulu
 * @Date 2020/2/15 13:36
 */
@Data
public class Result<T> {
    private String msg;
    private T data;
    private Integer status;



    public static Result success(Object data){
        Result re=new Result();
        re.data=data;
        re.status=0;
        re.msg="ok";
        return re;
    }
}
