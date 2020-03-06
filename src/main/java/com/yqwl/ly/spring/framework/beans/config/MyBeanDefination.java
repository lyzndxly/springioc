package com.yqwl.ly.spring.framework.beans.config;

public class MyBeanDefination {

    private String beanClassName;//全类名
    private String factoryBeanName;//在ioc容器中的key，1.默认首字母小写，2，自定义命名，3，类型

    public String getBeanClassName() {
        return beanClassName;
    }

    public void setBeanClassName(String beanClassName) {
        this.beanClassName = beanClassName;
    }

    public String getFactoryBeanName() {
        return factoryBeanName;
    }

    public void setFactoryBeanName(String factoryBeanName) {
        this.factoryBeanName = factoryBeanName;
    }
}
