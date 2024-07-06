package com.xl.redisaux.common.utils.compare;


import lombok.Getter;
import org.springframework.data.util.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class CompareUtilTest {
    public static void main(String[] args) {
        Pair<ComposeObj, ComposeObj> composeObjComposeObjPair = genData();
        ComposeObj source = composeObjComposeObjPair.getFirst();
        ComposeObj other = composeObjComposeObjPair.getSecond();
        CompareResult compareResult = CompareUtil.compareObjByGetter(source, other);
        //这里不同的值包括父对象
        List<CompareResult.CompareInfo> diffList = compareResult.getDiffList();

        String baseObjDiffInfo = compareResult.getBaseObjDiffInfo();
        System.out.printf(baseObjDiffInfo);

        //忽略son,list比对
        RegexFieldFilter son = RegexFieldFilter.of("son|list");
        System.out.println("==========ignore son,list field==========");
        CompareResult compareResult1 = CompareUtil.compareObjByGetter(source, other, son);
        String baseObjDiffInfo1 = compareResult1.getBaseObjDiffInfo();
        System.out.printf(baseObjDiffInfo1);
    }


    public static Pair<ComposeObj,ComposeObj> genData(){
        ComposeObj sourceObj = new ComposeObj("source");
        ComposeObj arrayObj = new ComposeObj("A1");
        sourceObj.arrays = new Object[]{arrayObj, new ComposeObj("A22")};
        sourceObj.list = Arrays.asList(new ComposeObj("C1"), new ComposeObj("C11"));
        Map<String, Object> map = new HashMap<>();
        map.put("test", "test");
        map.put("test2", "test2");
        sourceObj.map = map;

        ComposeObj  otherObj = new ComposeObj("other");
        ComposeObj arrayObj2 = new ComposeObj("A2");
        otherObj.arrays = new Object[]{arrayObj2, new ComposeObj("A22")};
        otherObj.list = Arrays.asList(new ComposeObj("C2"), new ComposeObj("C11"));
        Map<String, Object> map2 = new HashMap<>();
        map2.put("test", "test2");
        map2.put("test2", "test22");
        otherObj.map = map2;


        ComposeObj son=new ComposeObj("son");
        son.arrays= IntStream.of(1,2,3).boxed().toArray();
        sourceObj.son =son;
        son.list=Arrays.asList(2,2);
        ComposeObj son2=new ComposeObj("son2");
        son2.arrays= IntStream.of(1,2,2).boxed().toArray();
        son2.list=Arrays.asList(1,2);
        otherObj.son =son2;
        return Pair.of(sourceObj,otherObj);
    }

    //注意一定要有getter方法
    @Getter
    public static class ComposeObj {
        private String id;
        private Object[] arrays;

        private List<Object> list;

        private Map<String, Object> map;

        private ComposeObj son;

        private int idLength;

        public ComposeObj(String id) {
            this.id = id;
            this.idLength = id.length();
        }
    }
}
