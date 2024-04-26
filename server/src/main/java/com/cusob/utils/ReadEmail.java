package com.cusob.utils;

import com.cusob.service.impl.UserServiceImpl;

import java.io.InputStream;
import java.util.Scanner;

public class ReadEmail {
    public static String readwithcode(String path,String code){
        ClassLoader classLoader = UserServiceImpl.class.getClassLoader();
        String text= "";
        try {
            // 获取资源文件的输入流
            InputStream inputStream = classLoader.getResourceAsStream(path);
            if (inputStream != null) {
                // 使用 Scanner 一次性读取所有文本内容
                Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
                text = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
            } else {
                System.out.println("txt doesn't exist!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String before = text.substring(0, text.indexOf("---code---"));
        String after = text.substring(text.indexOf("---code---") + "---code---".length());
        return before+code+after;
    }

    public static String read(String path){
        ClassLoader classLoader = UserServiceImpl.class.getClassLoader();
        String text= "";
        try {
            // 获取资源文件的输入流
            InputStream inputStream = classLoader.getResourceAsStream(path);
            if (inputStream != null) {
                // 使用 Scanner 一次性读取所有文本内容
                Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
                text = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
            } else {
                System.out.println("txt doesn't exist!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }
}
