package com.opensource.redisaux.common.utils;

import com.opensource.redisaux.common.utils.IpCheckUtil;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author lulu
 * @Date 2020/7/1 17:49
 */
public class IpRuleHolder {
    private volatile PatriciaTrie<Set<String>> ipTrie;
    private ReadWriteLock lock;
    private String rule = null;

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = new String(rule);
    }

    public void addRule(String rule, String id) {
        if (ipTrie == null) {
            init(rule, id);
        }
        lock.writeLock().lock();
        Set<String> rules = IpCheckUtil.parseRule(rule);
        for (String s : rules) {
            Set<String> ids = ipTrie.get(s);
            if(ids==null){
                ids=new HashSet<>();
            }
            ids.add(id);
            ipTrie.put(s,ids);
        }
        lock.writeLock().unlock();

    }


    public synchronized void init(String rule, String id) {
        ipTrie = new PatriciaTrie();
        lock = new ReentrantReadWriteLock();
        this.rule = new String(rule);
    }


    public Set<String> getRuleFromIp(String ip, String id) {
        Set<String> result = new HashSet<>();
        lock.writeLock().lock();
        String[] split = ip.split("\\.");
        SortedMap<String, Set<String>> sortedMap = ipTrie.prefixMap(split[0] + "." + split[1]);
        sortedMap.forEach((k, v) -> {
            if (v.contains(id)) {
                result.add(k);
            }
        });
        lock.writeLock().unlock();
        return result;
    }
}
