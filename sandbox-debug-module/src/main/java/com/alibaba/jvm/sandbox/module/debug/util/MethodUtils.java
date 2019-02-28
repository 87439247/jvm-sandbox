package com.alibaba.jvm.sandbox.module.debug.util;


import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import static org.apache.commons.lang3.reflect.MethodUtils.getMatchingAccessibleMethod;

public class MethodUtils {
    /**
     * 方法缓存
     */
    private static ConcurrentHashMap<Long, Method> methods = new ConcurrentHashMap<>();

    /*
     * 泛型转换方法调用
     * 底层使用apache common实现
     */
    public static <T> T invokeMethod(final Object object,
                                     final String methodName,
                                     Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final long prime = 31;
        long result = 1;
        result = prime * result + object.getClass().hashCode();
        result = prime * result + methodName.hashCode();
        for (Object arg : args) {
            result = prime * result + (arg == null ? 0 : arg.getClass().hashCode());
        }
        Method method = methods.get(result);
        if (method == null) {
            args = ArrayUtils.nullToEmpty(args);
            Class<?>[] parameterTypes = ClassUtils.toClass(args);
            parameterTypes = ArrayUtils.nullToEmpty(parameterTypes);
            method = getMatchingAccessibleMethod(object.getClass(), methodName, parameterTypes);
            if (method == null) {
                throw new NoSuchMethodException("No such accessible method: " + methodName + "() on class: " + object.getClass().getName());
            }
            methods.put(result, method);
        }
        return (T) method.invoke(object, args);
    }
}
