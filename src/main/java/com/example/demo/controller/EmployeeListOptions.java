package com.example.demo.controller;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.MultiValueMap;

/** SPR-W5-D4-03/04: HTTPの一覧条件を検証してPageable/Sortへ変換する。 */
record EmployeeListOptions(boolean paged, int page, int size, Sort sort) {
    static EmployeeListOptions from(MultiValueMap<String,String> params) {
        boolean paged=params.containsKey("page") || params.containsKey("size");
        int page=integer(params,"page",0), size=integer(params,"size",20);
        if(page<0 || size<1) throw new IllegalArgumentException("pageは0以上、sizeは1以上で指定してください。");
        if((long)page*size>Integer.MAX_VALUE)
            throw new IllegalArgumentException("ページの取得位置が大きすぎます。");
        List<Sort.Order> orders=new ArrayList<>();
        // SPR-W5-D4-04: id/nameとasc/descだけ許可。複数sortを受け、同名はIDで安定化する。
        for(String value:params.getOrDefault("sort",List.of())) {
            String[] parts=value.split(",",-1);
            if(parts.length!=2 || !(parts[0].equals("id") || parts[0].equals("name"))
                    || !(parts[1].equals("asc") || parts[1].equals("desc")))
                throw new IllegalArgumentException("sortはid,nameとasc,descを使い、例：sort=name,ascで指定してください。");
            if(orders.stream().anyMatch(o->o.getProperty().equals(parts[0])))
                throw new IllegalArgumentException("同じsort項目を重複指定できません。");
            orders.add(new Sort.Order(Sort.Direction.fromString(parts[1]),parts[0]));
        }
        if(orders.stream().noneMatch(o->o.getProperty().equals("id"))) orders.add(Sort.Order.asc("id"));
        return new EmployeeListOptions(paged,page,size,Sort.by(orders));
    }
    private static int integer(MultiValueMap<String,String> params,String key,int fallback) {
        if(!params.containsKey(key)) return fallback;
        try {
            if(params.get(key).size()!=1) throw new NumberFormatException();
            return Integer.parseInt(params.getFirst(key));
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException(key+"は整数で一度だけ指定してください。");
        }
    }
    PageRequest pageable() { return PageRequest.of(page,size,sort); }
}
