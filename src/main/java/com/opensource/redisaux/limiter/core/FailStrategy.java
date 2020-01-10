package com.opensource.redisaux.limiter.core;

/**
 * @author: lele
 * @date: 2020/1/3 上午10:17
 * 失败策略，可以自定义
 */
public interface FailStrategy {
    Object handle(String msg);

   class DefaultStrategy implements FailStrategy{

       @Override
       public Object handle(String msg) {
           return msg+":too much request";
       }
   }
}