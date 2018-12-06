package com.alibaba.jvm.sandbox.module.debug.util;

import io.netty.util.internal.ConcurrentSet;
import org.apache.commons.lang3.StringUtils;

public class UrlUtils {
    private static ConcurrentSet<Character> words = new ConcurrentSet<>();

    private static ConcurrentSet<Character> numbers = new ConcurrentSet<>();

    static {
        String word = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ/?&=_";
        String number = "0123456789";
        for (char w : word.toCharArray()) {
            words.add(w);
        }
        for (char n : number.toCharArray()) {
            numbers.add(n);
        }
    }

    public static String rebuildPath(String uri) {
        if (StringUtils.isEmpty(uri)) {
            return "";
        }
        int start = uri.indexOf("://");
        if (start >= 0) {
            int pathStart = uri.indexOf("/", start + 3);
            uri = uri.substring(pathStart);
        }
        StringBuilder builder = new StringBuilder();
        boolean preIsNum = false;
        final String num = "(NUM)";
        for (char c : uri.toCharArray()) {

            if (words.contains(c)) {
                if (preIsNum) {
                    preIsNum = false;
                    builder.append(num);
                }
                builder.append(c);
            } else if (numbers.contains(c)) {
                preIsNum = true;
            } else {
                if (preIsNum) {
                    preIsNum = false;
                    builder.append(num);
                }
                final char sep = 'I';
                builder.append(sep);
            }
        }
        if (preIsNum) {
            builder.append(num);
        }
        return builder.toString();
    }

}
