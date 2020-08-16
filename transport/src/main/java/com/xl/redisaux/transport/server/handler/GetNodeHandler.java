package com.xl.redisaux.transport.server.handler;

import com.xl.redisaux.transport.consts.ServerPathConsts;
import com.xl.redisaux.transport.server.HeartBeatServerHandler;
import com.xl.redisaux.transport.vo.BaseVO;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;

/**
 * <pre>
 *  功能名称
 * </pre>
 *
 * @author tanjl11@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020/8/14 15:32
 *  修改内容:
 * </pre>
 */
public class GetNodeHandler implements UserRequestHandler {
    @Override
    public Object handle(HttpMethod method, String body, HttpHeaders headers) {
        return HeartBeatServerHandler.nodeVOMap.values();
    }

    @Override
    public String getURL() {
        return ServerPathConsts.GETNODES;
    }

    @Override
    public FullHttpRequest buildFullHttpRequest(BaseVO baseVO) {
        return null;
    }



}
