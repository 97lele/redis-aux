package com.xl.redisaux.common.utils.compare;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * <p>入口 {@link #compareObjByGetter(Object, Object, MethodFilter...)}</p>
 * <p>具体对比不通过的内容由 {@link CompareResult#diffList}提供</p>
 * <p>提供对比耗时统计,如果性能较差或多个比对结果可以使用异步比对 {@link #compareObjByGetterAsync(Object, Object, MethodFilter...)}</p>
 * <p>其中{@link CompareResult.CompareInfo#path}为 <a href="https://docs.spring.io/spring-framework/reference/core/expressions.html">spel表达式</a></p>
 * <p>{@link CompareJoiner} 提供多个比对&打印日志(默认不打印-概率为0)的功能</p>
 * @see MethodFilter getter方法过滤
 */
public final class CompareUtil {
    private CompareUtil() {
    }
 
    private static final String COLLECTION_SIGN = "C$";
    private static final String ARRAY_SIGN = "A$";
    private static final String FIELD_SIGN = "F$";
    private static final String KEY_SIGN = "K$";
 
    private static final Integer MAX_DEEP_SIZE = 200;
 
    /**
     * @param source        目前只支持同对象，不同对象目前可以转为同一个对象再比对
     * @param other
     * @param methodFilters {@link MethodFilter}
     * @param <T>
     * @return
     */
    public static <T> CompareResult compareObjByGetter(T source, T other, MethodFilter... methodFilters) {
        List<CompareResult.CompareInfo> diffList = new ArrayList<>();
        CompareContext compareContext = new CompareContext();
        compareContext.resCollect = diffList;
        compareContext.pathMessageStack = new ArrayDeque<>(MAX_DEEP_SIZE);
        compareContext.rootSourceObj = source;
        compareContext.rootOtherObj = other;
        long start = System.currentTimeMillis();
        boolean res = compareObjByGetter(source, other, compareContext, methodFilters);
        CompareResult compareResult = new CompareResult();
        compareResult.genCost(start);
        compareResult.diffList = diffList;
        compareResult.same = res;
        compareResult.other = other;
        compareResult.source = source;
        compareResult.methodFilterCostMill = compareContext.filterCost;
        return compareResult;
    }
 
    public static <T> CompletableFuture<CompareResult> compareObjByGetterAsync(T source, T other, MethodFilter... methodFilters) {
        return CompletableFuture.supplyAsync(() -> compareObjByGetter(source, other, methodFilters));
    }
 
    public static <T> CompletableFuture<CompareResult> compareObjByGetterAsync(T source, T other, Executor executor, MethodFilter... methodFilters) {
        return CompletableFuture.supplyAsync(() -> compareObjByGetter(source, other, methodFilters), executor);
    }
 
    public static CompareJoiner newJoiner(){
        return newJoiner(0);
    }
    public static CompareJoiner newJoiner(double logRate) {
        return new CompareJoiner(logRate);
    }
 
    public static class CompareJoiner {
        private static final Logger LOGGER = LoggerFactory.getLogger(CompareJoiner.class);
        private Map<String, Supplier<CompareResult>> suppliers = new HashMap<>();
        private boolean allSame = true;
        private Map<String, CompareResult> resultMap = new ConcurrentHashMap<>();
 
        private long costMills;
 
        private double logRate;
        private int index = 0;
        private CompareJoiner(double logRate) {
            this.logRate=logRate;
        }
 
        public CompareJoiner join(String compareId, Object source, Object other, MethodFilter... methodFilter) {
            suppliers.put(compareId, () -> compareObjByGetter(source, other, methodFilter));
            return this;
        }
 
        public CompareJoiner join(Object source, Object other, MethodFilter... methodFilters) {
            return join(String.valueOf(index++), source, other, methodFilters);
        }
 
        public List<CompareResult> getDiffResult() {
            List<CompareResult> results = new ArrayList<>();
            for (Map.Entry<String, CompareResult> entry : resultMap.entrySet()) {
                boolean same = entry.getValue().isSame();
                if (!same) {
                    results.add(entry.getValue());
                }
            }
            return results;
        }
 
        public boolean isAllSame() {
            return allSame;
        }
 
        public CompareResult getResultById(String id) {
            return resultMap.get(id);
        }
 
        public CompareJoiner executeParallel() {
            return executeParallel(500, TimeUnit.MILLISECONDS);
        }
 
        /**
         * 并行执行
         *
         * @param time
         * @param timeUnit
         */
        public CompareJoiner executeParallel(long time, TimeUnit timeUnit) {
            long start = System.currentTimeMillis();
            CountDownLatch count = new CountDownLatch(suppliers.values().size());
            for (Map.Entry<String, Supplier<CompareResult>> entry : suppliers.entrySet()) {
                Supplier<CompareResult> value = entry.getValue();
                String key = entry.getKey();
                if (!resultMap.containsKey(key)) {
                    CompletableFuture.supplyAsync(value).whenComplete((compareResult, throwable) -> {
                        resultMap.put(key, compareResult);
                        count.countDown();
                    });
                } else {
                    count.countDown();
                }
            }
            try {
                count.await(time, timeUnit);
                resultMap.values().forEach(e -> allSame &= e.same);
            } catch (InterruptedException e) {
                LOGGER.warn("compare parallel execute time out {} mills,use serial execute",timeUnit.convert(time,TimeUnit.MILLISECONDS));
                execute();
            }
            this.costMills =System.currentTimeMillis()-start;
            logDiff();
            return this;
        }
 
        /**
         * 串型执行
         */
        public CompareJoiner execute() {
            long start = System.currentTimeMillis();
            for (Map.Entry<String, Supplier<CompareResult>> stringSupplierEntry : suppliers.entrySet()) {
                String key = stringSupplierEntry.getKey();
                resultMap.computeIfAbsent(key, s -> {
                    CompareResult compareResult = stringSupplierEntry.getValue().get();
                    allSame &= compareResult.same;
                    return compareResult;
                });
            }
            this.costMills =System.currentTimeMillis()-start;
            logDiff();
            return this;
        }
 
        public CompareJoiner reset(){
            this.resultMap.clear();
            this.suppliers.clear();
            this.allSame =true;
            this.costMills =0;
            this.index = 0;
            return this;
        }
 
        private void logDiff(){
            if(logRate<=0){return;}
            if(logRate>=1||logRate>=ThreadLocalRandom.current().nextDouble(1)){
                for (Map.Entry<String, CompareResult> entry : resultMap.entrySet()) {
                    String key = entry.getKey();
                    if(!entry.getValue().same){
                        LOGGER.warn("compare joiner found difference,joiner key:[{}],different values [{}]",key,entry.getValue().getBaseObjDiffInfo());
                    }
                }
            }
        }
        @Override
        public String toString() {
            return "CompareJoiner{" +
                    "isAllSame=" + allSame +
                    ", costMills=" + costMills +
                    '}';
        }
    }
 
 
 
    protected static class CompareContext {
        //对比结果
        private List<CompareResult.CompareInfo> resCollect;
        //当前遍历的对象信息,用于定位
        private ArrayDeque<String> pathMessageStack;
        protected Object rootSourceObj;
        protected Object rootOtherObj;
        protected Object sourceObj;
        protected Object otherObj;
        protected Object sourceInvokeVal;
        protected Object otherInvokeVal;
        //过滤耗费时间
        private long filterCost;
 
        private String checkCycle() {
            Set<String> set = new LinkedHashSet<>();
            //出现重复退出
            while (set.add(pathMessageStack.removeLast())) {
            }
            String[] elements = new String[set.size()];
            Iterator<String> iterator = set.iterator();
            int index = 0;
            while (iterator.hasNext()) {
                elements[set.size() - 1 - index++] = iterator.next();
            }
            return getPath(elements);
        }
 
        protected String getPath(String[] elements) {
            Object obj = this.rootSourceObj == null ? this.rootOtherObj : this.rootSourceObj;
            String simpleName = obj.getClass().getSimpleName();
            StringBuilder builder = new StringBuilder(simpleName);
            if (elements == null) {
                elements = this.pathMessageStack.toArray(new String[0]);
            }
            for (int i = elements.length - 1; i >= 0; i--) {
                String cur = elements[i];
                String value = cur.substring(2);
                if (cur.startsWith(FIELD_SIGN)) {
                    builder.append(".").append(value);
                } else {
                    builder.append("[").append(value).append("]");
                }
            }
            return builder.toString();
        }
    }
 
    /**
     * 对比主入口
     *
     * @param source
     * @param other
     * @param compareContext
     * @param methodFilters
     * @param <T>
     * @return
     */
    private static <T> boolean compareObjByGetter(T source, T other, CompareContext compareContext, MethodFilter... methodFilters) {
        //对比路径太深或出现循环引用不支持
        if (compareContext.pathMessageStack.size() > MAX_DEEP_SIZE) {
            String path = compareContext.checkCycle();
            if (!StringUtils.isEmpty(path)) {
                //路径仅供参考，不一定准确
                throw new IllegalStateException(String.format("reference cycle happen,please check your object,path:%s", path));
            }
            throw new IllegalStateException(String.format("compare path over max size:%s,please check your object", MAX_DEEP_SIZE));
        }
        if(source==other){
            return true;
        }
        if (source == null || other == null) {
            generateResult(source, other, compareContext);
            return false;
        }
        if (!source.getClass().equals(other.getClass())) {
            throw new IllegalArgumentException(String.format("not the same object,class source:%s,class other:%s,path:%s", source.getClass(), other.getClass(),compareContext.getPath(null)));
        }
        //基本类型不再对比getter方法
        if (getObjType(source) != Object.class) {
            boolean isSame = compareBaseObj(source, other);
            if (!isSame) {
                generateResult(source, other, compareContext);
            }
            return isSame;
        }
        //复合类型
        if (isCollectionOrMapOrArray(source)) {
            return dealWithCMA(source, other, compareContext, methodFilters);
        }
        //对象类型，遍历对应的方法
        final boolean[] val = new boolean[]{true};
        ReflectionUtils.doWithMethods(source.getClass(), new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                String name = method.getName();
 
                if (method.getModifiers() == Modifier.PUBLIC && (name.startsWith("get") || name.startsWith("is"))) {
                    //有入参的getter不处理
                    if (method.getParameterTypes().length != 0) {
                        return;
                    }
                    String methodKey = method.getDeclaringClass().getName() + "#" + method.getName();
                    Object sourceInvokeVal = null;
                    Object otherInvokeVal = null;
                    try {
                        sourceInvokeVal = method.invoke(source);
                        otherInvokeVal = method.invoke(other);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    compareContext.otherObj = other;
                    compareContext.otherInvokeVal = otherInvokeVal;
                    compareContext.sourceObj = source;
                    compareContext.sourceInvokeVal = sourceInvokeVal;
                    //过滤,methodFilter是||关系
                    long start = System.currentTimeMillis();
                    try {
                        for (MethodFilter methodFilter : methodFilters) {
                            if (methodFilter.isSkipCompare(method, methodKey, compareContext)) {
                                return;
                            }
                        }
                    } finally {
                        compareContext.filterCost += System.currentTimeMillis() - start;
                    }
                    if (sourceInvokeVal == null && otherInvokeVal == null) {
                        return;
                    }
                    compareContext.pathMessageStack.push(String.format("%s%s", FIELD_SIGN,getFieldName(method.getName(), method.getDeclaringClass().getName())));
                    if (sourceInvokeVal == null || otherInvokeVal == null) {
                        generateResult(sourceInvokeVal, otherInvokeVal, compareContext);
                        val[0] = false;
                        compareContext.pathMessageStack.pop();
                        return;
                    }
                    if (isCollectionOrMapOrArray(sourceInvokeVal)) {
                        val[0] &= dealWithCMA(sourceInvokeVal, otherInvokeVal, compareContext, methodFilters);
                    } else {
                        //对象类型 or 基本类型
                        val[0] &= compareObjByGetter(sourceInvokeVal, otherInvokeVal, compareContext, methodFilters);
                    }
                    compareContext.pathMessageStack.pop();
                }
            }
        });
        return val[0];
    }
 
    /**
     * collection & map & array 处理
     *
     * @param sourceObj
     * @param otherObj
     * @param compareContext
     * @param methodFilters
     * @return
     */
    private static boolean dealWithCMA(Object sourceObj, Object otherObj, CompareContext compareContext, MethodFilter... methodFilters) {
        if(sourceObj==otherObj){
            return true;
        }
        boolean isDiff = true;
        if (sourceObj instanceof Collection) {
 
            Collection<?> sourceCollection = ((Collection<?>) sourceObj);
            Collection<?> otherCollection = ((Collection<?>) otherObj);
            //要求顺序,这里不做排序
            if (sourceCollection.size() != otherCollection.size()) {
                isDiff = false;
            } else {
                Iterator<?> sourceI = sourceCollection.iterator();
                Iterator<?> otherI = otherCollection.iterator();
                int index = 0;
                while (sourceI.hasNext()) {
                    Object sourceElement = sourceI.next();
                    Object otherElement = otherI.next();
                    //下一层不匹配的值
                    compareContext.pathMessageStack.push(String.format("%s%s", COLLECTION_SIGN, index++));
                    isDiff &= compareObjByGetter(sourceElement, otherElement, compareContext, methodFilters);
                    compareContext.pathMessageStack.pop();
                }
            }
        }
 
        if (sourceObj.getClass().isArray()) {
            Object[] sourceArray = (Object[]) sourceObj;
            Object[] otherArray = (Object[]) otherObj;
            if (sourceArray.length != otherArray.length) {
                isDiff = false;
            } else {
                for (int i = 0; i < sourceArray.length; i++) {
                    Object sourceElement = sourceArray[i];
                    Object otherElement = otherArray[i];
                    compareContext.pathMessageStack.push(String.format("%s%s", ARRAY_SIGN, i));
                    isDiff &= compareObjByGetter(sourceElement, otherElement, compareContext, methodFilters);
                    compareContext.pathMessageStack.pop();
                }
            }
        }
        if (sourceObj instanceof Map) {
            Map<?, ?> sourceMap = (Map) sourceObj;
            Map<?, ?> otherMap = (Map) otherObj;
            if (sourceMap.size() != otherMap.size()) {
                isDiff = false;
            } else {
                HashSet<?> otherKeySet = new HashSet<>(otherMap.keySet());
                for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
                    Object sourceKey = entry.getKey();
                    Object otherVal = otherMap.get(sourceKey);
                    otherKeySet.remove(sourceKey);
                    compareContext.pathMessageStack.push(String.format("%s%s", KEY_SIGN, sourceKey));
                    isDiff &= compareObjByGetter(entry.getValue(), otherVal, compareContext, methodFilters);
                    compareContext.pathMessageStack.pop();
                }
                if (!otherKeySet.isEmpty()) {
                    for (Object otherKey : otherKeySet) {
                        compareContext.pathMessageStack.push(String.format("%s%s", KEY_SIGN, otherKey));
                        isDiff &= compareObjByGetter(null, otherMap.get(otherKey), compareContext, methodFilters);
                        compareContext.pathMessageStack.pop();
                    }
                }
            }
        }
        if (!isDiff) {
            generateResult(sourceObj, otherObj, compareContext);
        }
        return isDiff;
    }
 
    /**
     * 生成spel路径，用于核对&查看
     *
     * @param sourceObj
     * @param otherObj
     * @param compareContext
     */
    private static void generateResult(Object sourceObj, Object otherObj, CompareContext compareContext) {
        CompareResult.CompareInfo compareInfo = new CompareResult.CompareInfo();
        compareInfo.sourceVal = sourceObj;
        compareInfo.otherVal = otherObj;
        compareInfo.path = compareContext.getPath(null);
        compareContext.resCollect.add(compareInfo);
    }
 
 
    private static <T> boolean compareBaseObj(T obj, T other) {
        //同一个对象直接通过
        if(obj==other){
            return true;
        }
        //数字类型
        if (obj instanceof Number && obj instanceof Comparable) {
            return ((Comparable<T>) obj).compareTo(other) == 0;
        }
        //其他类型
        return Objects.equals(obj, other);
    }
 
    /**
     * 后续比对有其他基本类型可以往里加
     * @param obj
     * @return
     */
    protected static Class<?> getObjType(Object obj) {
        if (obj instanceof Integer) {
            return Integer.class;
        } else if (obj instanceof String) {
            return String.class;
        } else if (obj instanceof BigDecimal) {
            return BigDecimal.class;
        } else if (obj instanceof Long) {
            return Long.class;
        } else if (obj instanceof Enum) {
            return Enum.class;
        } else if (obj instanceof Double) {
            return Double.class;
        } else if (obj instanceof Boolean) {
            return Boolean.class;
        }
        return Object.class;
    }
 
    private static boolean isCollectionOrMapOrArray(Object obj) {
        return obj instanceof Collection || obj instanceof Map || obj.getClass().isArray();
    }
 
    public static String getFieldName(String getMethodName,String className) {
        String get = "get";
        String is = "is";
        if (getMethodName.startsWith(get)) {
            getMethodName = getMethodName.substring(get.length());
        } else if (getMethodName.startsWith(is)) {
            getMethodName = getMethodName.substring(is.length());
        } else {
            throw new IllegalStateException(String.format("no found getter in class %s", className));
        }
        // 小写第一个字母
        return Character.isLowerCase(getMethodName.charAt(0)) ? getMethodName : Character.toLowerCase(getMethodName.charAt(0)) + getMethodName.substring(1);
    }
 
}