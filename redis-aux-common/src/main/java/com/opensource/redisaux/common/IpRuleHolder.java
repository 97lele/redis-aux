package com.opensource.redisaux.common;

import org.apache.commons.collections4.trie.PatriciaTrie;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author lulu
 * @Date 2020/7/1 17:49
 */
public class IpRuleHolder {
    private volatile PatriciaTrie<String> ipTrie;
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
            init(rule,id);
        } else {
            lock.writeLock().lock();
            ipTrie.putAll(IpCheckUtil.parseRule(rule,id));
            lock.writeLock().unlock();
        }
    }


    public synchronized void init(String rule,String id) {
        ipTrie = new PatriciaTrie();
        lock = new ReentrantReadWriteLock();
        Map<String, String> ruleMap = IpCheckUtil.parseRule(rule,id);
        this.rule = new String(rule);
        ipTrie.putAll(ruleMap);
    }


    public Set<String> getRuleFromIp(String ip,String id) {
        Set<String> result = new HashSet<>();
        lock.writeLock().lock();
        String[] split = ip.split("\\.");
        SortedMap<String,String> sortedMap = ipTrie.prefixMap(split[0]+"."+split[1]);
        sortedMap.forEach((k,v)->{
            if(v.equals(id)){
                result.add(k);
            }
        });
        lock.writeLock().unlock();
        return result;
    }
}
