package com.yqwl.ly.spring.framework.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface MyAutowired {
    /**
     * 判断是否是依赖注入项 默认true
     * @return
     */
    String value() default "";
}
