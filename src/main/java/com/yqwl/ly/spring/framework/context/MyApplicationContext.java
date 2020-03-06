package com.yqwl.ly.spring.framework.context;

import com.yqwl.ly.spring.framework.annotation.MyAutowired;
import com.yqwl.ly.spring.framework.annotation.MyController;
import com.yqwl.ly.spring.framework.annotation.MyService;
import com.yqwl.ly.spring.framework.beans.MyBeanWrapper;
import com.yqwl.ly.spring.framework.beans.config.MyBeanDefination;
import com.yqwl.ly.spring.framework.beans.support.MyBeanDefinationReader;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyApplicationContext {

    private MyBeanDefinationReader beanDefinationReader;

    private Map<String, MyBeanDefination> beanDefinationMap = new HashMap<>();
    //IOC容器
    private Map<String, MyBeanWrapper> factoryBeanInstanceCache = new HashMap<>();

    private Map<String,Object> factoryBeanObjectcache = new HashMap<>();
    public MyApplicationContext(){}

    public MyApplicationContext(String ...configLocations) throws Exception {
        //1.读取配置文件
        beanDefinationReader = new MyBeanDefinationReader(configLocations);

        //2.解析配置文件，将配置文件封装成BeanDefination对象

        List<MyBeanDefination> beanDefinationsList = beanDefinationReader.loadBeanDefinations();
        //3.将BeanDefination保存到容器Map中，key与IOC容器中的key beanName对应
        doRegisterBeanDefination(beanDefinationsList);
        //4.完成依赖注入
        doAutowired();

    }

    private void doAutowired() {
        for (Map.Entry<String,MyBeanDefination> entry : beanDefinationMap.entrySet()) {
            String key = entry.getKey();
            getBean(key);
        }
    }

    private void doRegisterBeanDefination(List<MyBeanDefination> beanDefinationsList) throws Exception{
        for (MyBeanDefination myBeanDefination:beanDefinationsList){

            if (this.beanDefinationMap.containsKey(myBeanDefination.getFactoryBeanName())){
                throw new Exception("The"+myBeanDefination.getFactoryBeanName()+"is exists!");
            }
            //完成类名注入
            this.beanDefinationMap.put(myBeanDefination.getFactoryBeanName(),myBeanDefination);
            //完成类型注入
            this.beanDefinationMap.put(myBeanDefination.getBeanClassName(),myBeanDefination);
        }
    }

    public Object getBean(String beanName){

        //获取beanDefination信息
        MyBeanDefination myBeanDefination = beanDefinationMap.get(beanName);
        //根基BeanDefination信息创建对象实例
        Object instance = instanceBean(beanName,myBeanDefination);
        //把创建出来的对象封装成BeanWrapper
        MyBeanWrapper myBeanWrapper = new MyBeanWrapper(instance);
        //把wrapper对象放入到IOC容器中
        this.factoryBeanInstanceCache.put(beanName,myBeanWrapper);
        //依赖注入
        populateBean(beanName,new MyBeanDefination(),myBeanWrapper);

        return this.factoryBeanInstanceCache.get(beanName).getWrapperInstance();
    }

    /**
     *
     * @param beanName
     * @param myBeanDefination
     * @param myBeanWrapper
     */
    private void populateBean(String beanName, MyBeanDefination myBeanDefination, MyBeanWrapper myBeanWrapper) {
        Object wrapperInstance = myBeanWrapper.getWrapperInstance();
        Class wrapperClass = myBeanWrapper.getWrapperClass();
        //判断类上是否有@MyController和@MyService
        if (!(wrapperClass.isAnnotationPresent(MyController.class)||wrapperClass.isAnnotationPresent(MyService.class))){return;}

        for (Field field : wrapperClass.getDeclaredFields()){
            if(!field.isAnnotationPresent(MyAutowired.class)){continue;}

            //获取MyAutowired注解对象
            MyAutowired myAutowiredAnnotation = field.getAnnotation(MyAutowired.class);
            //获取MyAutowired注解中的值
            String autowiredBeanName = myAutowiredAnnotation.value().trim();
            if ("".equals(autowiredBeanName)){

//                String[] split  = field.getType().getName().split("\\.");
//                int length = split.length;
//                if(length <1){continue;}
                autowiredBeanName = field.getType().getName();
            }
            //暴力访问 private
            field.setAccessible(true);

            //将entry.getValue()的field 设置成 singletonObject.get(beanName)，
            try {
                if(this.factoryBeanInstanceCache.get(autowiredBeanName)==null){continue;}
                field.set(wrapperInstance,this.factoryBeanInstanceCache.get(autowiredBeanName).getWrapperInstance());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     *  对象实例化过程
     * @param beanName  factoryBeanName
     * @param myBeanDefination
     * @return
     */
    private Object instanceBean(String beanName, MyBeanDefination myBeanDefination) {
        String beanClassName = myBeanDefination.getBeanClassName();

        Object instance =null;
        try {
            Class<?> clazz= Class.forName(beanClassName);
            instance = clazz.newInstance();
            this.factoryBeanObjectcache.put(beanName,instance);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return instance;
    }

    public Object getBean(Class clazz){
        return getBean(clazz.getName());
    }

    /**
     *
     * @return 返回beanDefinationMap的key组成的数组
     */

    public String[] getBeanDefiantionNames(){
        return beanDefinationMap.keySet().toArray(new String[this.beanDefinationMap.size()]);
    }

    public int getBeanDefinationCount(){
        return beanDefinationMap.size();
    }
}


