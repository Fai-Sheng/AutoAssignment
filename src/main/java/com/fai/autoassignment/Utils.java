package com.fai.autoassignment;

import android.content.Intent;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by PVer on 2018/7/2.
 */

public class Utils {

    public static String convertToString(Object obj){
        return String.valueOf(obj);
    }

    public static int stringToInt(String str){
        return Integer.parseInt(str);
    }

    public static long stringToLong(String str){
        return Long.parseLong(str);
    }

    public static boolean isLong(Class cls){
        if(null == cls){
            return false;
        }
        if(cls.getSimpleName().equals("long")){
            return true;
        } else if(cls.getSimpleName().equals("Long")){
            return true;
        }
        return false;
    }

    public static boolean isInt(Class cls)
    {
        if(null == cls){
            return false;
        }
        if(cls.getSimpleName().equals("int")){
            return true;
        } else if(cls.getSimpleName().equals("Integer")){
            return true;
        }
        return false;
    }

    //判断是否是String，基本类型或者包装类，确定是否可以转换
    public static boolean isConvertable(Class cls)
    {
        if(null == cls){
            return false;
        }

        if(cls.isPrimitive()){
            return true;
        } else if(cls.equals(Integer.class) || cls.equals(Long.class)){
            return true;
        }

        return cls.isPrimitive();
    }

    public static boolean isString(Class cls){
        if(null == cls){
            return false;
        }
        return cls.equals(String.class);
    }


    public static boolean isListEmpty(List list) {
        if (list == null || list.size() == 0) {
            return true;
        }
        return false;
    }

    public static Class getListGeneric(Field field) {
        if (field == null) {
            return null;
        }
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Class cls = (Class) pt.getActualTypeArguments()[0];
            return cls;
        }
        return null;
    }


    public static List<Object> arrayToList(Object[] objs) {
        if (objs == null || objs.length <= 0) {
            return null;
        }
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < objs.length; i++) {
            list.add(objs[i]);
        }
        return list;
    }
}
