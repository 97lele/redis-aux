package com.xl.redisaux.transport.server.handler;

import com.xl.redisaux.common.utils.ClassUtil;
import com.xl.redisaux.transport.annonation.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author lulu
 * @Date 2020/8/14 15:03
 * 逻辑处理分发类
 */
public class DispatchHandler {
    Logger log = LoggerFactory.getLogger(DispatchHandler.class);
    Map<String, UserRequestHandler> map;
    private boolean loaded;
    private String prefix;
    private String scanPath;

    private enum DispatchHandlerHolder {
        HOLDER;
        private DispatchHandler instance;

        DispatchHandlerHolder() {
            instance = new DispatchHandler();
        }
    }

    public static DispatchHandler getInstance() {
        return DispatchHandlerHolder.HOLDER.instance;
    }

    public UserRequestHandler dispatch(String uri) {
        UserRequestHandler userRequestHandler = map.get(uri);
        return userRequestHandler;
    }

    public synchronized void init(String prefix, String scanPath) throws IllegalAccessException, InstantiationException {
        if (isLoaded()) {
            log.warn("dispatchHandler has been load,prefix-{},scanPath-{}", getPrefix(), getScanPath());
            return;
        }
        map = new HashMap<>();
        Set<Class<?>> classSet = ClassUtil.extractPackageClass(scanPath);
        for (Class<?> clazz : classSet) {
            if (clazz.isAnnotationPresent(RequestHandler.class) && clazz.isAssignableFrom(UserRequestHandler.class)) {
                Object o = clazz.newInstance();
                UserRequestHandler handler = (UserRequestHandler) o;
                map.put(prefix + handler.getURL(), handler);
            }
        }
        this.scanPath = scanPath;
        this.prefix = prefix;
        loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getScanPath() {
        return scanPath;
    }
}
