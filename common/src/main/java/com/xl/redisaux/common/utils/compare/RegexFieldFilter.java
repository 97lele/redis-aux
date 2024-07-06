package com.xl.redisaux.common.utils.compare;

import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 通过简单的表达式
 * "*abc|xxx" ->标识以abc结尾或xxx的属性忽略，不做比对,*代表通配符
 */
public class RegexFieldFilter implements MethodFilter {
    private static final Cache<String, Pattern> REGEX_MAP = CacheBuilder.newBuilder()
            .softValues().maximumSize(2048).build();
 
    private final List<String> rules;
 
    private static final String WILDCARD = "*";
    //是否缓存结果，如果调用次数较多（比如属性为list，且实际场景数量较多）可以用启用
    private boolean cacheResult = false;
    //方法key
    private Cache<String, Boolean> resultMap;
 
    private final Set<String> equalsField = new HashSet<>();
 
    private final Map<String, String> prefixFieldMap = new HashMap<>();
 
    private final Map<String, String> suffixFieldMap = new HashMap<>();
 
    private RegexFieldFilter(String regex, boolean cacheResult) {
        this.cacheResult = cacheResult;
        rules = Splitter.on("|").splitToList(regex);
        for (String rule : rules) {
            //普通比对
            if (!rule.contains(WILDCARD)) {
                equalsField.add(rule);
            }
            //首尾通配符特殊逻辑
            if (onlyOneWildcard(rule)) {
                if (rule.startsWith(WILDCARD)) {
                    suffixFieldMap.put(rule, rule.substring(1));
                }
                if (rule.endsWith(WILDCARD)) {
                    prefixFieldMap.put(rule, rule.substring(0, rule.length() - 1));
                }
            }
        }
        if (cacheResult) {
            resultMap = CacheBuilder.newBuilder().softValues().maximumSize(1024).build();
        }
 
    }
 
    public static RegexFieldFilter of(String regex, boolean cacheResult) {
        return new RegexFieldFilter(regex, cacheResult);
    }
 
    public static RegexFieldFilter of(String regex) {
        return of(regex, false);
    }
 
 
    private boolean canSkip(String rule, String fieldName) {
        if (rule.contains(WILDCARD)) {
            if (suffixFieldMap.containsKey(rule)) {
                return fieldName.endsWith(suffixFieldMap.get(rule));
            }
            if (prefixFieldMap.containsKey(rule)) {
                return fieldName.startsWith(prefixFieldMap.get(rule));
            }
            //在中间或多个通配符
            String replace = StringUtils.replace(rule, WILDCARD, ".*");
            Pattern pattern = REGEX_MAP.asMap().computeIfAbsent(replace, Pattern::compile);
            return pattern.matcher(fieldName).matches();
        }
        return equalsField.contains(fieldName);
    }
 
    private boolean onlyOneWildcard(String rule) {
        if (!rule.contains(WILDCARD)) {
            return false;
        }
        return rule.indexOf(WILDCARD, rule.indexOf(WILDCARD) + 1) == -1;
    }
 
    @Override
    public boolean isSkipCompare(Method method, String methodKey, CompareUtil.CompareContext context) {
        return cacheResult ? resultMap.asMap().computeIfAbsent(methodKey, s -> judgeSkip(method)) : judgeSkip(method);
 
    }
 
    private boolean judgeSkip(Method method) {
        if (!CollectionUtils.isEmpty(rules)) {
            String name = method.getName();
            String fieldName = CompareUtil.getFieldName(name, method.getDeclaringClass().getCanonicalName());
            for (String rule : rules) {
                if (canSkip(rule, fieldName)) {
                    return true;
                }
            }
        }
        return false;
    }
 
 
}