package com.yqwl.ly.spring.action.service;

import com.yqwl.ly.spring.action.service.Iservice.IDemoService;
import com.yqwl.ly.spring.framework.annotation.MyService;

@MyService
public class DemoServiceImpl implements IDemoService {
    @Override
    public String get(String name) {
        return "My name is "+name;
    }

    @Override
    public String get(String name,  String addr) {
        return "My name is "+name+" ,My 住址 is "+addr;
    }
}
