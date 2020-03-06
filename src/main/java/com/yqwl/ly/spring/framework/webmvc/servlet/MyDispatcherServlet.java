package com.yqwl.ly.spring.framework.webmvc.servlet;

import com.yqwl.ly.spring.framework.annotation.MyController;
import com.yqwl.ly.spring.framework.annotation.MyRequestMapping;
import com.yqwl.ly.spring.framework.annotation.MyRequestParam;
import com.yqwl.ly.spring.framework.context.MyApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class MyDispatcherServlet extends HttpServlet {

    //
    private Map<String,Method> handlerMapping = new HashMap<>();


    MyApplicationContext myApplicationContext;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }
    //运行阶段
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //调度
        try {
            doDispatcher(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception detail:"+ Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     *
     * @param req
     * @param resp
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws InvocationTargetException, IllegalAccessException {

        //        request.getServletPath()-----/user/register.action
        //        request.getContextPath()-----/testWeb
        //        request.getRequestURI()-----/testWeb/user/register.action
        //        request.getRequestURL()-----http://localhost:8080/testWeb/user/register.action

        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();

        requestURI = requestURI.replace(contextPath, "").replaceAll("/+", "/");
        //判断前端请求的地址 handlerMapping中是否存在
        if (!this.handlerMapping.containsKey(requestURI)){
            try {
                resp.getWriter().write("404 not found!!");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //获取前端传递的参数
        Map<String,String[]> parameterMap = req.getParameterMap();

        Method method = this.handlerMapping.get(requestURI);
        //反射获取方法中的参数类型数组
        Class<?>[] parameterTypes = method.getParameterTypes();
        //创建参数列表数组
        Object[] params = new Object[parameterTypes.length];
        //遍历获取参数类型并赋值
        for (int i = 0;i < parameterTypes.length;i++) {
            //获取第I个参数类型并赋值
            Class<?> parameterType = parameterTypes[i];
            //判断第I个参数类型并赋值
            if(parameterType == HttpServletRequest.class){
                params[i] = req;
                continue;
            }else if(parameterType ==HttpServletResponse.class){
                params[i] = resp;
                continue;
            }else if (parameterType ==String.class){//可能能有多个注解
                //如果是字符串类型，可能有多个参数，反射获取参数注解数组->二维数组
                Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                //遍历这个二维数组，第一维是注解类型，二维是注解中的参数
                for ( Annotation[] annotation :parameterAnnotations){
                        //获取第I个注解并
                        for (Annotation a:parameterAnnotations[i]){
                            //判断注解类型，并获取注解中的值
                            if(a instanceof MyRequestParam){
                                String paramName = ((MyRequestParam) a).value();

                                if (!"".equals(paramName.trim())){
                                    String value = Arrays.toString(parameterMap.get(paramName))
                                            .replaceAll("\\[|\\]","")
                                            .replaceAll("\\s",",");

                                    params[i] = value;
                                }
                            }
                        }
                }


            }
        }

        //根据method获取beanName
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());

        //反射执行方法
        method.invoke(myApplicationContext.getBean(method.getDeclaringClass()),params);


    }

    @Override
    public void destroy() {
        System.out.println("destroy ......................s");
    }

    //初始化阶段
    @Override
    public void init(ServletConfig config) throws ServletException {

//下面的在MyApplicationContext类中实现
//        //=========IOC ========================
//        //1.加载配置文件
//        doLoadConfig(config.getInitParameter("contextConfiguration"));
//        //2.扫描包路径，获得相关的类
//        doScanner(contextConfig.getProperty("scanPackage"));
//        //3.实例化扫描到的类，并将其放入到IOC容器中
//        doInstance();
//
//        //===========DI=========================
//        //4.完成依赖注入
//        doAutowired();

        try {
            myApplicationContext = new MyApplicationContext(config.getInitParameter("contextConfiguration"));
        } catch (Exception e) {
            e.printStackTrace();
        }


        //===============MVC====================
        //5.初始化handlerMapping
        doInitHandlerMapping();

        System.out.println("my spring framework is init ..............");
    }

    /**
     *  完成handlerMapping
     */
    private void doInitHandlerMapping() {
        if (myApplicationContext.getBeanDefinationCount()==0){return;}
        //遍历ioc容器
        String[] beanDefiantionNames = myApplicationContext.getBeanDefiantionNames();

        for (String beanDefiantionName : beanDefiantionNames) {

            Object beanObject = myApplicationContext.getBean(beanDefiantionName);
            Class<?> clazz = beanObject.getClass();

            if (!clazz.isAnnotationPresent(MyController.class)){ continue;}

            String baseUrl = "";
            //类上含有MyRequestMapping注解的类,获取其value
            if (clazz.isAnnotationPresent(MyRequestMapping.class)){
                MyRequestMapping myRequestMappingAnnotation = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = myRequestMappingAnnotation.value();
            }
            //方法上含有的MyRequestMapping注解的，获取其value
            for (Method method:clazz.getMethods()){
                if(!method.isAnnotationPresent(MyRequestMapping.class)){return;}

                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);

                String url = ("/"+ baseUrl+"/"+requestMapping.value()).replaceAll("/+","/");

                handlerMapping.put(url,method);

                System.out.println("Mapped:"+url+","+method);
            }


        }
    }

    /**
     * 自动注入
     */
//    private void doAutowired() {
//        if (singletonObject.isEmpty()){return;}
//
//        //遍历IOC容器
//        for (Map.Entry<String,Object> entry:singletonObject.entrySet()){
//            //Declared 所有的特定的字段，private、public、protected、default，字段要不要注入取决于有没有注解
//            for (Field field:entry.getValue().getClass().getDeclaredFields()){
//                //如果字段上没有myAutowire注解，就跳过
//                if (!field.isAnnotationPresent(MyAutowired.class)){return;}
//
//                //获取MyAutowired注解对象
//                MyAutowired myAutowiredAnnotation = field.getAnnotation(MyAutowired.class);
//                //获取MyAutowired注解中的值
//                String beanName = myAutowiredAnnotation.value().trim();
//                if ("".equals(beanName)){
//
//                    String[] split  = field.getType().getName().split("\\.");
//                    int length = split.length;
//                    if(length <1){continue;}
//                    beanName = toLowerFirstCase(split[split.length-1]);
//                }
//                //暴力访问 private
//                field.setAccessible(true);
//
//                //将entry.getValue()的field 设置成 singletonObject.get(beanName)，
//                try {
//                    field.set(entry.getValue(),singletonObject.get(beanName));
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

    /**
     * 实例化
     */
//    private void doInstance() {
//        if (classNameList.isEmpty()){return;}
//
//        try {
//            for (String className:classNameList){
//                //第一次通过反射获取对应的类
//                Class<?> clazz = Class.forName(className);
//
//                if (clazz.isAnnotationPresent(MyController.class)){
//
//                    String clazzSimpleName = clazz.getSimpleName();
//
//                    String beanName = toLowerFirstCase(clazzSimpleName);
//                    //第二次通过反射获取对象
//                    Object instance = clazz.newInstance();
//
//                    singletonObject.put(beanName,instance);
//
//                }else if (clazz.isAnnotationPresent(MyService.class)){
//
//                    String clazzSimpleName = clazz.getSimpleName();
//                    //首字母小写
//                    String beanName = toLowerFirstCase(clazzSimpleName);
//                    //获取myService注解中的值，并重新赋值给beanName
//                    MyService myService = clazz.getAnnotation(MyService.class);
//                    if(!"".equals(myService.value())){
//                        beanName = myService.value();
//                    }
//                    //第二次通过反射获取对象
//                    Object instance = clazz.newInstance();
//                    singletonObject.put(beanName,instance);
//
//                    //将实例的类型作为beanName
//                    for (Class c:clazz.getInterfaces()){
//                        String IBeanName = toLowerFirstCase(c.getName());
//                        if (singletonObject.containsKey(beanName)){
//                            continue;
//                        }
//                        singletonObject.put(IBeanName,instance);
//                    }
//                }else {continue;}
//
//
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//    }

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
     * 扫描类路径
     * @param scanPackage 配置文件中 package
     */
//    private void doScanner(String scanPackage) {
//        //包路径替换成文件路径  (com.yqwl.ly)
//        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
//        //获得类路径
//        File classPath = new File(url.getFile());
//        //遍历classPath下所有的文件，就是所有的类名
//        for (File file:classPath.listFiles()){
//
//            if (file.isDirectory()){
//                doScanner(scanPackage+"."+file.getName());
//            }else {
//                //跳过不是以.class为结尾的文件
//                if (!file.getName().endsWith(".class")){continue;}
//                //获取完整类名并存储到List中
//                String className = scanPackage + "."+file.getName().replace(".class", "");
//                classNameList.add(className);
//            }
//        }
//    }




    /**
     * 加载配置文件
     * @param contextConfiguration 是web.xml中的 中的param-value值
     */
//    private void doLoadConfig(String contextConfiguration) {
//        //从类路径下读取配置文件
//        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfiguration);
//
//        try {
//            contextConfig.load(resourceAsStream);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }finally {
//            if (null!=resourceAsStream){
//                try {
//                    resourceAsStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
}
