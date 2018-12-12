package com.alibaba.jvm.sandbox.module.debug.util;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

public class FieldUtils {

    /**
     * field缓存
     */
    static ConcurrentHashMap<Long, Field> fields = new ConcurrentHashMap<>();

    /*
     * 泛型转换方法调用
     * 底层使用apache common实现
     */
    public static <T> T invokeField(final Object object,
                                    final String fieldName) throws IllegalAccessException, NoSuchFieldException {
        final long prime = 31;
        long result = 1;
        result = prime * result + object.getClass().hashCode();
        result = prime * result + fieldName.hashCode();

        Field field = fields.get(result);
        if (field == null) {
            field = org.apache.commons.lang3.reflect.FieldUtils.getField(object.getClass(), fieldName, true);
            if (field == null) {
                throw new NoSuchFieldException(object.getClass().getName() + " could not find " + fieldName);
            }
            fields.put(result, field);
        }
        return (T) field.get(object);
    }
}
