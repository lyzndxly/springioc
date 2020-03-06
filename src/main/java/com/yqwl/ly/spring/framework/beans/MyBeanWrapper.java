package com.yqwl.ly.spring.framework.beans;

public class MyBeanWrapper {
    private Object wrapperInstance;
    private Class wrapperClass;

    public Object getWrapperInstance() {
        return wrapperInstance;
    }
    public MyBeanWrapper(){}
    public void setWrapperInstance(Object wrapperInstance) {
        this.wrapperInstance = wrapperInstance;
    }

    public Class getWrapperClass() {
        return wrapperClass;
    }

    public void setWrapperClass(Class wrapperClass) {
        this.wrapperClass = wrapperClass;
    }

    public MyBeanWrapper(Object instance) {
        this.wrapperInstance =instance;
        this.wrapperClass = instance.getClass();
    }
}
