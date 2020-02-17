package com.opensource.redisaux.common;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author lulu
 * @Date 2020/2/15 21:00
 */
public class IpCheckUtil {
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

    private static Set<String> parseRule(String ruleStr) {
        Set<String> addList = new HashSet<>();
        String[] ruleList = ruleStr.split(";");
        for (String rule : ruleList) {
            if (rule.contains("*")) {// 处理通配符 *
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
                    addList.add(ip);
                }
                // 处理 网段 xxx.xxx.xxx./24
            } else if (rule.contains("/")) {
                addList.add(rule);
            } else {// 处理单个 ip 或者 范围
                if (IpCheckUtil.validate(rule)) {
                    addList.add(rule);
                }
            }
        }
        return addList;
    }

    /**
     * 获取真实ip地址，避免获取代理ip
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ipAddress = request.getHeader("x-forwarded-for");
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0|| "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0|| "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (ipAddress.equals("127.0.0.1")) {
                // 根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                    ipAddress = inet.getHostAddress();
                } catch (UnknownHostException e) {
                    System.out.println("出现异常:"+e.toString());
                }
            }
        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        // "***.***.***.***".length() = 15
        if (ipAddress != null && ipAddress.length() > 15) {
            if (ipAddress.indexOf(",") > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
            }
        }
        if("0:0:0:0:0:0:0:1".equals(ipAddress)){
            ipAddress="127.0.0.1";
        }
        return ipAddress;
    }


    public static boolean isFit(String ip, String rule) {
        Set<String> ipList = parseRule(rule);
        if (ipList.isEmpty() || ipList.contains(ip)) {
            return true;
        }
        for (String allow : ipList) {
            // 处理 类似 192.168.0.0-192.168.2.1
            if (allow.indexOf("-") > -1) {
                String[] tempAllow = allow.split("-");
                String[] from = tempAllow[0].split("\\.");
                String[] end = tempAllow[1].split("\\.");
                String[] tag = ip.split("\\.");
                boolean check = true;
                // 对IP从左到右进行逐段匹配
                for (int i = 0; i < 4; i++) {
                    int s = Integer.valueOf(from[i]);
                    int t = Integer.valueOf(tag[i]);
                    int e = Integer.valueOf(end[i]);
                    if (!(s <= t && t <= e)) {
                        check = false;
                        break;
                    }
                }
                if (check) {
                    return true;
                }
                // 处理 网段 xxx.xxx.xxx./24
            } else if (allow.contains("/")) {
                return matches(allow, ip);
            }
        }
        return false;
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
