package com.alibaba.jvm.sandbox.module.debug.util;

import org.apache.commons.lang3.StringUtils;

/**
 * url 处理工具
 *
 * @author yuanyue@staff.hexun.com
 */
public class UrlUtils {

    private UrlUtils(){
        // do nothing
    }

    /**
     * 将url中的变量值替换掉,能使之在cat报表里面,成为一个可以分类的标准
     *
     * @param url url通常不带host和协议
     * @return 处理后的地址
     */
    public static String rebuildPath(String url) {
        if (StringUtils.isEmpty(url)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        final char queryStart = '=';
        final char queryStop = '&';
        final char questionMark = '?';
        boolean queryStarted = false;
        boolean queryReplacing = false;
        boolean digitReplacing = false;
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == questionMark) {
                queryStarted = true;
            }
            //是否已经到了问号,问号后的逻辑是保留参数名去掉参数值.问号前path的逻辑是判断数字
            if (queryStarted) {
                if (c == queryStop) {
                    queryReplacing = false;
                }
                if (!queryReplacing) {
                    builder.append(c);
                }
                if (c == queryStart) {
                    queryReplacing = true;
                }
            } else {
                if (c < 48 || c > 57) {
                    digitReplacing = false;
                }
                if (!digitReplacing && c >= 48 && c <= 57) {
                    digitReplacing = true;
                    builder.append("(num)");
                }
                if (!digitReplacing) {
                    builder.append(c);
                }
            }
        }
        return builder.toString();
    }
}
