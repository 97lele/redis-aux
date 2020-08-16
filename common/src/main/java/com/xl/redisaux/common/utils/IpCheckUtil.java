package com.xl.redisaux.common.utils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author lulu
 * @Date 2020/2/15 21:00
 * 用于ip匹配
 */
public class IpCheckUtil {
    //ip地址模式匹配
    private static Pattern pattern = Pattern
            .compile("(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\." + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\."
                    + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\." + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})");


    private static boolean validate(String ip) {
        String[] temp = ip.split("-");
        for (String s : temp) {
            if (!pattern.matcher(s).matches()) {
                return false;
            }
        }
        return true;
    }

    private static List<String> complete(String arg) {
        List<String> com = new ArrayList<String>();
        int len = arg.length();
        if (len == 1) {
            com.add("0;255");
        } else if (len == 2) {
            String s1 = complete(arg, 1);
            if (s1 != null) {
                com.add(s1);
            }
            String s2 = complete(arg, 2);
            if (s2 != null) {
                com.add(s2);
            }
        } else {
            String s1 = complete(arg, 1);
            if (s1 != null) {
                com.add(s1);
            }
        }
        return com;
    }

    private static String complete(String arg, int length) {
        String from = "";
        String end = "";
        if (length == 1) {
            from = arg.replace("*", "0");
            end = arg.replace("*", "9");
        } else {
            from = arg.replace("*", "00");
            end = arg.replace("*", "99");
        }
        if (Integer.valueOf(from) > 255) {
            return null;
        }
        if (Integer.valueOf(end) > 255) {
            end = "255";
        }
        return from + ";" + end;
    }


    /**
     * 解析规则字符返回字符列表
     * @param ruleStr
     * @return
     */
    public static Set<String> parseRule(String ruleStr) {
        Set<String> ruleSet=new HashSet<>();
        String[] ruleList = ruleStr.split(";");
        for (String rule : ruleList) {
            // 处理通配符 *
            if (rule.contains("*")) {
                String[] ips = rule.split("\\.");
                String[] from = new String[]{"0", "0", "0", "0"};
                String[] end = new String[]{"255", "255", "255", "255"};
                List<String> tem = new LinkedList<>();
                for (int i = 0; i < ips.length; i++) {
                    if (ips[i].indexOf("*") > -1) {
                        tem = IpCheckUtil.complete(ips[i]);
                        from[i] = null;
                        end[i] = null;
                    } else {
                        from[i] = ips[i];
                        end[i] = ips[i];
                    }
                }
                StringBuilder fromIP = new StringBuilder();
                StringBuilder endIP = new StringBuilder();
                for (int i = 0; i < 4; i++) {
                    if (from[i] != null) {
                        fromIP.append(from[i]).append(".");
                        endIP.append(end[i]).append(".");
                    } else {
                        fromIP.append("[*].");
                        endIP.append("[*].");
                    }
                }
                fromIP.deleteCharAt(fromIP.length() - 1);
                endIP.deleteCharAt(endIP.length() - 1);
                for (String s : tem) {
                    String ip = fromIP.toString().replace("[*]", s.split(";")[0]) + "-"
                            + endIP.toString().replace("[*]", s.split(";")[1]);
                    ruleSet.add(ip);

                }
                // 处理 网段 xxx.xxx.xxx./24
            } else if (rule.contains("/")) {
                ruleSet.add(rule);
            } else {// 处理单个 ip 或者 范围
                if (IpCheckUtil.validate(rule)) {
                    ruleSet.add(rule);
                }
            }
        }
        return ruleSet;
    }

    /**
     * 获取ip
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if (ip.indexOf(",") != -1) {
                ip = ip.split(",")[0];
            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }


    public static boolean isFit(String ip, Set<String> ipSet) {
        if ( ipSet.contains(ip)) {
            return true;
        }
        for (String allow : ipSet) {
            // 处理 类似 192.168.0.0-192.168.2.1
            if (allow.indexOf("-") > -1) {
                return ipExistsInRange(ip, allow);
                // 处理 网段 xxx.xxx.xxx./24
            } else if (allow.contains("/")) {
                return matches(allow, ip);
            }
        }
        return false;
    }


    public static boolean ipExistsInRange(String ip, String ipSection) {

        ipSection = ipSection.trim();

        ip = ip.trim();

        int idx = ipSection.indexOf('-');

        String beginIP = ipSection.substring(0, idx);

        String endIP = ipSection.substring(idx + 1);

        return getIp2long(beginIP) <= getIp2long(ip) && getIp2long(ip) <= getIp2long(endIP);

    }

    public static long getIp2long(String ip) {

        ip = ip.trim();

        String[] ips = ip.split("\\.");

        long ip2long = 0L;

        for (int i = 0; i < 4; ++i) {

            ip2long = ip2long << 8 | Integer.parseInt(ips[i]);

        }

        return ip2long;

    }

    private static boolean matches(String net, String ip) {
        int mask = Integer.valueOf(net.split("/")[1]);
        mask = 0xFFFFFFFF << (32 - mask);
        String address = net.split("/")[0];
        int baseIp = parseAddr(address);
        return (baseIp & mask) == (parseAddr(ip) & mask);
    }

    private static int parseAddr(String addr) {
        String[] array = addr.split("\\.");
        int ip = 0;
        for (int i = 0; i < array.length; i++) {
            int part = (Integer.parseInt(array[i]) & 0xFF) << (24 - 8 * i);
            ip = ip | part;
        }
        return ip;
    }

}
