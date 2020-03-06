package com.yqwl.ly.spring.framework.beans.support;

import com.yqwl.ly.spring.framework.beans.config.MyBeanDefination;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MyBeanDefinationReader {


    private Properties contextConfig = new Properties();

    private List<String>  registerBeanClasses = new ArrayList<>();
    public MyBeanDefinationReader(){}
    public MyBeanDefinationReader(String ...configLocations) {
        //1、定位并加载配置文件
        doLoadConfig(configLocations);

        //2、读取配置文件的信息
        doScanner(contextConfig.getProperty("scanPackage"));

        //解析配置文件的 内容解析为BeanDefination


      }

    private void doScanner(String scanPackage) {
                //包路径替换成文件路径  (com.yqwl.ly)

                URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
                //获得类路径
                File classPath = new File(url.getFile());
                //遍历classPath下所有的文件，就是所有的类名
                for (File file:classPath.listFiles()){
                    if (file.isDirectory()){
                        doScanner(scanPackage+"."+file.getName());
                    }else {
                        //跳过不是以.class为结尾的文件
                        if (!file.getName().endsWith(".class")){continue;}
                        //获取完整类名并存储到List中
                        String className = scanPackage + "."+file.getName().replace(".class", "");
                        registerBeanClasses.add(className);

            }
        }
    }

    public List<MyBeanDefination> loadBeanDefinations() {
        List<MyBeanDefination> result = new ArrayList<>();

        for (String name:registerBeanClasses){

            Class<?> clazz = null;
            try {
                clazz = Class.forName(name);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            //如果是接口类型就跳过，因接口无法创建对象
            if (clazz.isInterface()){continue;}

            result.add(doCreateBeanDefination(toLowerFirstCase(clazz.getSimpleName()),clazz.getName()));
            //获得当前类型的接口，如果有接口的情况,(考虑到可以通过接口注入的情况)
            for (Class cl:clazz.getInterfaces()){
                result.add(doCreateBeanDefination(cl.getName(),clazz.getName()));
            }
        }

        return result;
    }


    private MyBeanDefination doCreateBeanDefination(String toLowerFirstCase, String name) {

        MyBeanDefination myBeanDefination = new MyBeanDefination();
        //类型注入的前提
        myBeanDefination.setBeanClassName(name);
        //类名注入的前提
        myBeanDefination.setFactoryBeanName(toLowerFirstCase);
        return myBeanDefination;
    }

    /**
     *  将类名首字母小写
     */

    private String toLowerFirstCase(String clazzSimpleName){

        char[] chars = clazzSimpleName.toCharArray();
        char ch = chars[0];
        if (ch >= 'A' && ch <= 'Z') {
            chars[0]+=32;
            return String.valueOf(chars);
        }
        return clazzSimpleName;

    }
    /**
     * 加载配置文件
     * @param contextConfiguration
     */
    private void doLoadConfig(String ...contextConfiguration) {
        //从类路径下读取配置文件
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfiguration[0].replaceAll("classpath:",""));
        try {
            contextConfig.load(resourceAsStream);
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (null!=resourceAsStream){
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }



}
