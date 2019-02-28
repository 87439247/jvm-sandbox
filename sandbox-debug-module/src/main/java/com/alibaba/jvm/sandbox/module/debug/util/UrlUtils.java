package com.alibaba.jvm.sandbox.module.debug.util;

import io.netty.util.internal.ConcurrentSet;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class UrlUtils {
    private static ConcurrentSet<Character> words = new ConcurrentSet<>();

    private static ConcurrentSet<Character> numbers = new ConcurrentSet<>();

    static {
        String word = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ/.?&=_";
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
        final String num = "(N)";
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

    public static void main(String[] args) {
        String url = "http://abc.com:8230/asdfsdf/asde.jsp?param1=123829djkfed&param2=f232fkd";
        System.out.println(rebuildPath(url));
        if (url.contains("?")) {
            Pattern pattern = Pattern.compile("&?.*?\\=(.*?)");

            StringBuilder builder = new StringBuilder();
            char start = '=';
            char stop = '&';
            char questionMark = '?';
            boolean queryStarted = false;
            boolean replacing = false;
            for (int i = 0; i < url.length(); i++) {
                char c = url.charAt(i);
                if (c == questionMark) {
                    queryStarted = true;
                }

                if (c == stop) {
                    replacing = false;
                }
                if (!replacing) {
                    builder.append(c);
                }
                if (c == start) {
                    replacing = true;
                }
            }

            System.out.println(builder);

//            pattern.
//
//
//            String[] split = url.split("\\?");
//            StringBuilder result = new StringBuilder(split[0]);
//            if(split.length >= 2){
//                String params = split[1];
//                String[] args1 = params.split("=");
//            }
        }
    }

}
