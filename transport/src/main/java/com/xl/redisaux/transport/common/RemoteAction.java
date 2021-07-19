package com.xl.redisaux.transport.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@NoArgsConstructor
@ToString
public  class RemoteAction<T> {
    //header
    protected int actionCode;
    protected boolean isResponse;
    protected int requestId;
    //body
    protected T body;

    protected Class<T> clazz;

    protected static AtomicInteger ID_GENERATOR =new AtomicInteger(0);

    protected static ObjectMapper OBJECT_MAPPER =new ObjectMapper();

   protected RemoteAction(int actionCode,boolean isResponse,int requestId,T body){
       this.isResponse=isResponse;
       this.actionCode=actionCode;
       this.requestId=requestId;
       this.body=body;
       this.clazz= (Class<T>) body.getClass();
   }

   public static<T> RemoteAction<T>  request(SupportAction supportAction,T body){
       int requestId = ID_GENERATOR.incrementAndGet();
       return new RemoteAction(supportAction.getActionCode(),false,requestId,body);
   }

   public static<T> RemoteAction<T> response(SupportAction supportAction,T body,int requestId){
       return new RemoteAction(supportAction.getActionCode(),true,requestId,body);
   }

    public void encode(ByteBuf byteBuf) {
        byteBuf.writeInt(actionCode);
        byteBuf.writeBoolean(isResponse);
        byteBuf.writeInt(requestId);
        try {
            byteBuf.writeBytes(OBJECT_MAPPER.writeValueAsBytes(body));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("encode error:",e);
        }
    }

    public static<R> R getBody(Class<R> clazz,RemoteAction remoteAction) {
        return (R)remoteAction.getBody();
    }

    public static<T> RemoteAction decode(ByteBuf byteBuf) {
        int actionCode = byteBuf.readInt();
        boolean isResponse = byteBuf.readBoolean();
        int requestId = byteBuf.readInt();
        String s = byteBuf.toString(StandardCharsets.UTF_8);
        Class<T> actionClass = (Class<T>) SupportAction.getActionClass(actionCode, isResponse);
        T body = null;
        try {
            body = OBJECT_MAPPER.readValue(s, actionClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("decode error:",e);
        }
        RemoteAction<T> action = new RemoteAction();
        action.actionCode = actionCode;
        action.isResponse = isResponse;
        action.requestId = requestId;
        action.body=body;
        action.clazz=actionClass;
        return action;
    }

    public void setResponse(boolean response) {
        isResponse = response;
    }

    public void setBody(T body) {
        this.body = body;
        this.clazz= (Class<T>) body.getClass();
    }
}
