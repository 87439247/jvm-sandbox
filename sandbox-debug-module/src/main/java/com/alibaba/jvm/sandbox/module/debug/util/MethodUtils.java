package com.alibaba.jvm.sandbox.module.debug.util;


import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MethodUtils {
    /*
     * 泛型转换方法调用
     * 底层使用apache common实现
     */
    public static <T> T invokeMethod(final Object object,
                                     final String methodName,
                                     final Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return (T) org.apache.commons.lang3.reflect.MethodUtils.invokeMethod(object, methodName, args);
    }


//    /*
//     * 泛型转换方法调用
//     * 底层使用apache common实现
//     */
//    public static <T> T invokeMethod1(final Object object, final String methodName, final Object... args) {
//        return (T) MethodAccess.get(object.getClass()).invoke(object, methodName, args);
//    }
//
//    private static ConcurrentMap<Class, MethodAccess> methodMap = new ConcurrentHashMap<>();
}
