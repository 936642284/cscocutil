package com.yzycoc.custom;

import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Date;

/**
 * @program: cscocutil
 * @description: 字符集
 * @author: yzy
 * @create: 2020-08-24 20:48
 * @Version 1.0
 **/
public class Utf8Util {
    /**
     * 获取字符串的unicode编码
     * 汉字“木”的Unicode 码点为Ox6728
     *
     * @param s 木
     * @return \ufeff\u6728  \ufeff控制字符 用来表示「字节次序标记（Byte Order Mark）」不占用宽度
     */
    public static String stringToUnicode(String str) {
        StringBuffer sb = new StringBuffer();
        char[] c = str.toCharArray();
        for (int i = 0; i < c.length; i++) {
            sb.append("\\u" + Integer.toHexString(c[i]));
        }
        return sb.toString();
    }
    /**
     * unicode转字符串
     *
     * @param unicode
     * @return
     */
    public static String unicodeToString(String unicode) {
        StringBuffer sb = new StringBuffer();
        String[] hex = unicode.split("\\\\u");
        for (int i = 1; i < hex.length; i++) {
            int index = Integer.parseInt(hex[i], 16);
            sb.append((char) index);
        }
        return sb.toString();
    }

    /***
     * 字符集转换 数据库认可
     * @param key
     * @return
     */
    public static String Mybatislike(String key) {
        if(StringUtils.isEmpty(key)) {
            return key;
        }
        return key.replace("\\", "\\\\").replace("%", "\\%").replace("％", "\\％")
                .replace("_", "\\_").replace("＿", "\\＿").replace("'", "''");
    }


    /***
     * 删除指定文件夹下所有文件
     * 删除两个小时钱的文件夹
     * @param path 文件夹完整绝对路径
     * @param date 删除两个小时钱的文件夹
     * @return
     */
    public static  boolean delAllFile(String path, Date date) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                if (new Date(temp.lastModified()).before(date)) {
                }
                temp.delete();
            }
            if (temp.isDirectory()) {
                //delAllFile(path + "/" + tempList[i]);// 先删除文件夹里面的文件
                flag = true;
            }
        }
        return flag;
    }
}
