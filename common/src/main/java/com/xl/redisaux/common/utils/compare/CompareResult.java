package com.xl.redisaux.common.utils.compare;

import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
@ToString
public class CompareResult {
    protected boolean same;
    protected Object source;
    protected Object other;
    protected List<CompareInfo> diffList;
 
 
    //=====额外信息，可以不关注====
    //整体对比耗时
    private long totalCostMill;
 
    //方法过滤耗时
    protected long methodFilterCostMill;
 
 
 
    protected void genCost(long start) {
        this.totalCostMill = System.currentTimeMillis() - start;
    }
 
   
 
    @Getter
    public static class CompareInfo {
        protected Object sourceVal;
        protected Object otherVal;
        protected String path;
 
        protected boolean isShow() {
            if (sourceVal == null || otherVal == null) {
                return true;
            }
            if (CompareUtil.getObjType(sourceVal) != Object.class) {
                return true;
            }
            if (sourceVal instanceof Collection) {
                return ((Collection<?>) sourceVal).size() != ((Collection<?>) otherVal).size();
            }
            if (sourceVal instanceof Map) {
                return ((Map<?, ?>) sourceVal).size() != ((Map<?, ?>) otherVal).size();
            }
            if (sourceVal.getClass().isArray()) {
                return ((Object[]) sourceVal).length != ((Object[]) otherVal).length;
            }
            return false;
        }
 
        @Override
        public String toString() {
            return String.format("path:%s,source:[%s],other:[%s]", path, sourceVal, otherVal);
        }
    }
 
 
    public String getBaseObjDiffInfo() {
        return getBaseObjDiffInfo("\n");
    }
 
    /**
     * 过滤出为空的父对象或基本对象，原生的diffList会包含父对象，不方便查看
     *
     * @param seperator
     * @return
     */
    public String getBaseObjDiffInfo(String seperator) {
        if (!same) {
            StringBuilder builder = new StringBuilder();
            diffList.stream().filter(CompareInfo::isShow).forEach(v -> builder.append(v).append(seperator));
            return builder.toString();
        }
        return "";
    }
 
}