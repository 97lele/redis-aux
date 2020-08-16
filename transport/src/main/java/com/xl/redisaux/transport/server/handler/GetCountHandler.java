package com.xl.redisaux.transport.server.handler;

import com.alibaba.fastjson.JSON;
import com.xl.redisaux.transport.annonation.RequestHandler;
import com.xl.redisaux.transport.consts.ServerPathConsts;
import com.xl.redisaux.transport.vo.BaseVO;
import io.netty.handler.codec.http.*;

import java.net.URI;
import java.net.URISyntaxException;

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
 *  修改日期: 2020/8/14 16:21
 *  修改内容:
 * </pre>
 */
@RequestHandler
public class GetCountHandler implements UserRequestHandler {
    @Override
    public Object handle(HttpMethod method, String body, HttpHeaders headers) {
        BaseVO baseVO = JSON.parseObject(body, BaseVO.class);
        FullHttpRequest request = buildFullHttpRequest(baseVO);
        String s = null;
        try {
            s = RequestSender.sendRequest(request, baseVO);
        } catch (Exception e) {
        }
        return s;
    }

    @Override
    public String getURL() {
        return ServerPathConsts.GETCOUNT;
    }

    @Override
    public FullHttpRequest buildFullHttpRequest(BaseVO vo) {
        String uri = String.format("http://%s:%d%s?groupId=%s", vo.ip, vo.port, ServerPathConsts.PERFIX + getURL(), vo.groupId);
        FullHttpRequest request = null;
        try {
            request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, new URI(uri).toASCIIString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return request;
    }
}
