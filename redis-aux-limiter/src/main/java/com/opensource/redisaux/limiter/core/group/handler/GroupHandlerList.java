package com.opensource.redisaux.limiter.core.group.handler;

import com.opensource.redisaux.common.RedisAuxException;
import com.opensource.redisaux.limiter.core.group.GroupHandler;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author lulu
 * @Date 2020/2/17 10:13
 * 按权重从大到小排序
 */
public class GroupHandlerList implements Iterable<GroupHandler> {

    private Node head;

    public void add(GroupHandler a) {
        Node n = new Node(a);
        if (head != null) {
            Node p = head;
            while (p.next != null) {
                if (p.next.val.getOrder() == p.next.val.getOrder()) {
                    throw new RedisAuxException("order equal error");
                }
            }
            n.next = head.next;
        } else {
            head = n;
        }

    }

    public void sort() {
        this.head = sort(head);
    }

    public boolean isSort() {
        if (head != null) {
            Node p = head;
            while (p.next != null) {
                if (p.val.getOrder() < p.next.val.getOrder()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void remove(GroupHandler groupHandler) {
        Node p = head;
        if (p != null) {
            while (p.next != null) {
                if (p.next.val.getOrder() == groupHandler.getOrder()) {
                    p.next = p.next.next;
                    break;
                }
            }
        }

    }


    private Node sort(Node head) {
        //没有一个节点
        if (head == null) {
            return null;
        }
        //有一个节点
        if (head.next == null) {
            return head;
        }
        //有两个以上的节点

        Node middle = split(head);

        return mergeTwoLists(sort(head), sort(middle));

    }

    //把两个有序链表合并
    private Node mergeTwoLists(Node l1, Node l2) {
        Node prehead = new Node(null);
        Node prev = prehead;
        while (l1 != null && l2 != null) {
            if (l1.val.getOrder() >= l2.val.getOrder()) {
                prev.next = l1;
                l1 = l1.next;
            } else {
                prev.next = l2;
                l2 = l2.next;
            }
            prev = prev.next;
        }
        prev.next = l1 == null ? l2 : l1;
        return prehead.next;
    }


    //分成两部分的部分
    private Node split(Node head) {
        Node quick = head;
        Node slow = head;
        Node pre = null;
        while (quick != null) {
            pre = slow;
            slow = slow.next;
            quick = quick.next;
            if (quick != null) {
                quick = quick.next;
            }
        }
        pre.next = null;
        return slow;
    }

    @Override
    public Iterator<GroupHandler> iterator() {
        return new Iterator() {
            Node p = head;

            @Override
            public boolean hasNext() {
                return p != null && p.next == null;
            }

            @Override
            public Object next() {
                Object res = p.val;
                p = p.next;
                return res;
            }
        };

    }


    public static class Node {
        GroupHandler val;
        Node next;

        Node(GroupHandler x) {
            val = x;
            next = null;
        }
    }
}