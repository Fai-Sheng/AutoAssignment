package com.fai.autoassignment.imp;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import com.fai.autoassignment.annotations.EntityParam;
import com.fai.autoassignment.annotations.Param;
import com.fai.autoassignment.core.Resolver;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashSet;

/**
 * Created by DaSheng on 2018/6/4.
 */

// 1.两个相同的CParam 的name值

// 2.只有goal上面有 name值

// 3.两个字段上面都没有值

// 4.存在内部类对象 的情况如何处理

// 5.内部类的对象 的值 赋值给一个goal的普通值

// 6.多重内部类 对象（特殊情况）

// 7.数组和对象如何判断，String类型的判断

// 8.多个 内部的对象有相同的 字段

//9.加入是 两个 Array  的 Item 不是 出自同一个类 ,如何赋值

//10. String 的数组如何处理 基本类型 的 数组 ；比较子元素的class是否相同，想同就直接赋值

//11.类型不同 如何赋值 强制转换会不会有影响！！！int long  基本类型还是要拎出来淡出判断一下

public class FieldResolver implements Resolver {

    private Object src;
    private Object goal;
    private HashSet<Field> allFieldSet;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public <T, K> K execSetParam(T src, K goal) {
        this.src = src;
        this.goal = goal;
        try {
            resolve(src,goal);
        } catch (NoSuchFieldException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return goal;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void resolve(Object src,Object goal) throws NoSuchFieldException, InstantiationException, IllegalAccessException {
        if(null == src){
            return;
        }
        if (null == goal) {
            return;
        }

        Field[] fields = goal.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            resolveField(field,goal,src);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void resolveField(Field goalField,Object goal, Object src) throws NoSuchFieldException, IllegalAccessException, InstantiationException {
        if (null == goalField) {
            return;
        }

        if(resolveObjField(goalField,src,goal)){
           return;
        }

        if(resolveParamAnnotationWithExtraValue(goalField,src,goal)) {
            return;
        }

        if(resolveParamAnnotationField(goalField,src,goal)){
            return;
        }

        resolveNormalField(goalField,src,goal);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean resolveObjField(Field goalField , Object src, Object goal) throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        Class goalFieldClass = goalField.getType();
        EntityParam goalEntityParam = (EntityParam) goalFieldClass.getDeclaredAnnotation(EntityParam.class);
        //1.对象Field
        if (null != goalEntityParam) {
            String goalEntityParamName = goalEntityParam.name();
            if(TextUtils.isEmpty(goalEntityParamName)){
                goalEntityParamName = goalField.getName(); //如果是空值，默认就是name
            }
            Field srcFieldWithParam = findFieldWithEntityAnnotation(src,goalEntityParamName);
            if(srcFieldWithParam != null){
                Object goalFieldObj = goalFieldClass.newInstance();
                goalField.set(goal,goalFieldObj);
                resolve(srcFieldWithParam.get(src),goalField.get(goal));
            }
            return true;
        }
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean resolveParamAnnotationWithExtraValue(Field goalField , Object src, Object goal) throws NoSuchFieldException, IllegalAccessException, InstantiationException {
        //2.带Param注解 并且fromEntity有值 的注解的Field

        if(null == src || null == goal || null == goalField){
            return false;
        }

        Param param = goalField.getDeclaredAnnotation(Param.class);
        if(null == param){
            return false;
        }

        String[] strArray = param.fromEntityField();
        if(strArray.length <= 0){
            return false;
        }

        Object srcDestObj = src;
        for(String str : strArray){
            Field field = srcDestObj.getClass().getField(str);
            srcDestObj = field.get(srcDestObj);
        }
        diffResolveArrayOrElse(goalField,srcDestObj,goal);
        return true;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean resolveParamAnnotationField(Field goalField , Object src, Object goal) throws NoSuchFieldException, IllegalAccessException, InstantiationException {
        //3.带Param的普通Field
        if (null != goalField.getDeclaredAnnotation(Param.class)) {
            //这个Field带有 Param
            Param goalParam = goalField.getDeclaredAnnotation(Param.class);
            String goalParamName = goalParam.name();

            if(TextUtils.isEmpty(goalParamName)){
                goalParamName = goalField.getName();
            }

            Field srcField = findFieldWithFieldAnnotation(src,goalParamName);
            if(null != srcField){
                Object srcDestObj = srcField.get(src);
                diffResolveArrayOrElse(goalField,srcDestObj,goal);
            } else {
                //寻找 没有 Param 但是 Field的name相同的
                Field srcField2 = src.getClass().getDeclaredField(goalParamName);
                Object srcDestObj = srcField2.get(src);
                diffResolveArrayOrElse(goalField,srcDestObj,goal);
            }
            return true;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void resolveNormalField(Field goalField , Object src, Object goal) throws NoSuchFieldException, IllegalAccessException, InstantiationException {
        //4.不带注解的普通字段
        String goalFieldName = goalField.getName();
        Field srcField = src.getClass().getField(goalFieldName);
        Object srcDestObj = srcField.get(src);
        diffResolveArrayOrElse(goalField,srcDestObj,goal);
    }

    /**
     * 判断 是不是 数组
     * @param goalField
     * @param srcArrayObj
     * @param goal
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void resolveArray(Field goalField, Object srcArrayObj , Object goal) throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        if(null == goalField || null == srcArrayObj || null == goal){
            return;
        }
        if(!srcArrayObj.getClass().isArray() || !goalField.getType().isArray())  //判断是不是数组
        {
            return;
        }
        Class srcComponentType = srcArrayObj.getClass().getComponentType();
        Class goalComponentType = goalField.getType().getComponentType();

        if(srcComponentType.equals(goalComponentType)){
            goalField.set(goal,srcArrayObj);
            return;
        }

        Object[] arr = (Object[]) srcArrayObj;
        //通过goalField的信息去寻找src中的对应的值
        int length = arr.length;
        Object[] goalObjArray = (Object[]) Array.newInstance(goalComponentType,length);
        for(int i = 0;i < length;i ++){
            Object goalComponent = goalComponentType.newInstance();
            resolve(arr[i],goalComponent);
            goalObjArray[i] = goalComponent;
        }
        goalField.set(goal,goalObjArray);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Field findFieldWithEntityAnnotation(Object obj , String name)
    {
        if(null == obj || TextUtils.isEmpty(name)){
            return null;
        }
        Field[] fs = obj.getClass().getDeclaredFields();
        //先判断带有相同name的 EntityParam
        for(Field f : fs){
            f.setAccessible(true);
            Class fieldClass = f.getType();
            EntityParam entityParam = (EntityParam) fieldClass.getDeclaredAnnotation(EntityParam.class);
            if(null != entityParam){
                String entityParamName = entityParam.name();
                if(TextUtils.isEmpty(entityParamName)){
                    entityParamName = fieldClass.getSimpleName();
                }
                if(entityParamName.equals(name)){
                    return f;
                }
            }
        }
        //再去寻找不带EntityParam，但是类名和EntityParam的name相同的
        for(Field ff : fs) {
            ff.setAccessible(true);
            Class fieldCls = ff.getType();
            String clsName = fieldCls.getSimpleName();
            if(clsName.equals(name)){
                return ff;
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Field findFieldWithFieldAnnotation(Object obj , String name)
    {
        if(null == obj || TextUtils.isEmpty(name)){
            return null;
        }
        Field[] fields = obj.getClass().getDeclaredFields();
        //先找 带Param的字段
        for(Field f : fields){
            f.setAccessible(true);
            if(null != f.getDeclaredAnnotation(Param.class) && f.getDeclaredAnnotation(Param.class).name().equals(name)){
                return f;
            }
        }
        //在找没有Param的 普通字段
        for(Field ff : fields) {
            ff.setAccessible(true);
            String fieldName = ff.getName();
            if(fieldName.equals(name)){
                return ff;
            }
        }
        return null;
    }

    /**
     * 检查是不是 数组
     * @param goalField
     * @param srcFieldObj
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void diffResolveArrayOrElse(Field goalField, Object srcFieldObj, Object goal) throws IllegalAccessException, NoSuchFieldException, InstantiationException {
        if(null == goalField || null == srcFieldObj){
            return;
        }
        Class goalCls = goalField.getType();
        if(goalCls.isArray()){
            resolveArray(goalField,srcFieldObj,goal);
        } else {
            goalField.set(goal,srcFieldObj);
        }
    }
}
