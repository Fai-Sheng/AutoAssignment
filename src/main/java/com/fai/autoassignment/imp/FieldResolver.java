package com.fai.autoassignment.imp;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;

import com.fai.autoassignment.Utils;
import com.fai.autoassignment.annotations.EntityParam;
import com.fai.autoassignment.annotations.Param;
import com.fai.autoassignment.core.Resolver;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by DaSheng on 2018/6/4.
 *
 * 1.任意的int long String转换
 * 2.List和数组的任意赋值，支持不同的Item对象赋值
 * 3.两个普通变量的赋值
 * 4.层级相同之间的赋值，层级不同的赋值
 * 5.不管层级多深都可以赋值 内部类必须是静态内部类
 * 6.
 */

public class FieldResolver implements Resolver {

    private Object src;
    private Object goal;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public <T, K> K execSetParam(T src, K goal) {
        this.src = src;
        this.goal = goal;
        try {
            resolve(src, goal);
        } catch (NoSuchFieldException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return goal;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void resolve(Object src, Object goal) throws NoSuchFieldException, InstantiationException, IllegalAccessException {
        if (null == src) {
            return;
        }
        if (null == goal) {
            return;
        }

        Field[] fields = goal.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            resolveField(field, goal, src);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void resolveField(Field goalField, Object goal, Object src) throws NoSuchFieldException, IllegalAccessException, InstantiationException {
        if (null == goalField) {
            return;
        }

        if (resolveObjField(goalField, src, goal)) {
            return;
        }

        if (resolveParamAnnotationWithExtraValue(goalField, src, goal)) {
            return;
        }

        if (resolveParamAnnotationField(goalField, src, goal)) {
            return;
        }

        resolveNormalField(goalField, src, goal);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean resolveObjField(Field goalField, Object src, Object goal) throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        Class goalFieldClass = goalField.getType();
        EntityParam goalEntityParam = (EntityParam) goalFieldClass.getDeclaredAnnotation(EntityParam.class);
        //1.对象Field
        if (null != goalEntityParam) {
            String goalEntityParamName = goalEntityParam.name();
            if (TextUtils.isEmpty(goalEntityParamName)) {
                goalEntityParamName = goalField.getName(); //如果是空值，默认就是name
            }
            Field srcFieldWithParam = findFieldWithEntityAnnotation(src, goalEntityParamName);
            if (srcFieldWithParam != null) {
                Object goalFieldObj = goalFieldClass.newInstance();
                goalField.set(goal, goalFieldObj);
                resolve(srcFieldWithParam.get(src), goalField.get(goal));
            }
            return true;
        }
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean resolveParamAnnotationWithExtraValue(Field goalField, Object src, Object goal) throws NoSuchFieldException, IllegalAccessException, InstantiationException {
        //2.带Param注解 并且fromEntity有值 的注解的Field

        if (null == src || null == goal || null == goalField) {
            return false;
        }

        Param param = goalField.getDeclaredAnnotation(Param.class);
        if (null == param) {
            return false;
        }

        String[] strArray = param.fromEntityField();
        if (strArray.length <= 0) {
            return false;
        }

        Object srcDestObj = src;
        for (String str : strArray) {
            Field field = null;
            try{
                field = srcDestObj.getClass().getField(str);
            } catch (Exception e){
            }
            if(field == null){
                return false;
            }
            srcDestObj = field.get(srcDestObj);
        }
        diffResolveArrayOrElse(goalField, srcDestObj, goal);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean resolveParamAnnotationField(Field goalField, Object src, Object goal) throws NoSuchFieldException, IllegalAccessException, InstantiationException {
        //3.带Param的普通Field
        if (null != goalField.getDeclaredAnnotation(Param.class)) {
            //这个Field带有 Param
            Param goalParam = goalField.getDeclaredAnnotation(Param.class);
            String goalParamName = goalParam.name();

            if (TextUtils.isEmpty(goalParamName)) {
                goalParamName = goalField.getName();
            }

            Field srcField = findFieldWithFieldAnnotation(src, goalParamName);
            if (null != srcField) {
                Object srcDestObj = srcField.get(src);
                diffResolveArrayOrElse(goalField, srcDestObj, goal);
            } else {
                //寻找 没有 Param 但是 Field的name相同的
                Field srcField2 = null;
                try {
                    srcField2 = src.getClass().getDeclaredField(goalParamName);
                } catch (Exception e){
                }
                if(null == srcField2){
                    return false;
                }
                Object srcDestObj = srcField2.get(src);
                diffResolveArrayOrElse(goalField, srcDestObj, goal);
            }
            return true;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void resolveNormalField(Field goalField, Object src, Object goal) throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        //4.不带注解的普通字段
        String goalFieldName = goalField.getName();
        Field srcField = null;
        try{
            srcField = src.getClass().getDeclaredField(goalFieldName);
            srcField.setAccessible(true);
        } catch (Exception e){
        }
        if (srcField != null) {
            Object srcDestObj = srcField.get(src);
            diffResolveArrayOrElse(goalField, srcDestObj, goal);
        }
    }

    /**
     * 判断 是不是 数组
     *
     * @param goalField
     * @param srcObj
     * @param goal
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void resolveGoalArray(Field goalField, Object srcObj, Object goal) throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        if (null == goalField || null == srcObj || null == goal) {
            return;
        }
        if (!goalField.getType().isArray())  //判断 goalField是不是数组
        {
            return;
        }
        Class goalComponentType = goalField.getType().getComponentType();

        //TODO src 是数组
        if (srcObj.getClass().isArray()) {
            Class srcComponentType = srcObj.getClass().getComponentType();
            if (srcComponentType.equals(goalComponentType)) {
                goalField.set(goal, srcObj);
                return;
            }
            Object[] arr = (Object[]) srcObj;
            //通过goalField的信息去寻找src中的对应的值
            int length = arr.length;
            Object goalObjArray =  Array.newInstance(goalComponentType, length);
            for (int i = 0; i < length; i++) {
                Object goalComponent = goalComponentType.newInstance();
                resolve(arr[i], goalComponent);
                Array.set(goalObjArray,i,goalComponent);
            }
            try {
                goalField.set(goal, goalObjArray);
            }catch (Exception e){
            }
            return;
        }

        if (isList(srcObj.getClass())) {           //srcObj是List 要转为Array 再赋值
            List<Object> srcList = (List<Object>) srcObj;
            if (Utils.isListEmpty(srcList)) {
                return;
            }
            Object goalArray = Array.newInstance(goalComponentType,srcList.size());   //创建数组，必须使用Array，不然无法赋值
            Class srcListItemCls = srcList.get(0).getClass();
            if (srcListItemCls.equals(goalComponentType)) {
                for(int i=0;i < srcList.size();i ++){
                    Array.set(goalArray,i,srcList.get(i));
                }
            } else {
                for (int i = 0; i < srcList.size(); i++) {
                    Object goalItem = goalComponentType.newInstance();
                    resolve(srcList.get(i), goalItem);
                    Array.set(goalArray,i,goalItem);
                }
            }
            try{
                goalField.set(goal,goalArray);
            }catch (Exception e){

            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Field findFieldWithEntityAnnotation(Object obj, String name) {
        if (null == obj || TextUtils.isEmpty(name)) {
            return null;
        }
        Field[] fs = obj.getClass().getDeclaredFields();
        //先判断带有相同name的 EntityParam
        for (Field f : fs) {
            f.setAccessible(true);
            Class fieldClass = f.getType();
            EntityParam entityParam = (EntityParam) fieldClass.getDeclaredAnnotation(EntityParam.class);
            if (null != entityParam) {
                String entityParamName = entityParam.name();
                if (TextUtils.isEmpty(entityParamName)) {
                    entityParamName = fieldClass.getSimpleName();
                }
                if (entityParamName.equals(name)) {
                    return f;
                }
            }
        }
        //再去寻找不带EntityParam，但是类名和EntityParam的name相同的
        for (Field ff : fs) {
            ff.setAccessible(true);
            Class fieldCls = ff.getType();
            String clsName = fieldCls.getSimpleName();
            if (clsName.equals(name)) {
                return ff;
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Field findFieldWithFieldAnnotation(Object obj, String name) {
        if (null == obj || TextUtils.isEmpty(name)) {
            return null;
        }
        Field[] fields = obj.getClass().getDeclaredFields();
        //先找 带Param的字段
        for (Field f : fields) {
            f.setAccessible(true);
            if (null != f.getDeclaredAnnotation(Param.class) && f.getDeclaredAnnotation(Param.class).name().equals(name)) {
                return f;
            }
        }
        //再找 没有Param的 普通字段
        Field f = null;
        try{
            f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
        } catch (Exception e){
        }
        return f;
    }

    /**
     * 检查是不是 数组
     *
     * @param goalField
     * @param srcFieldObj
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void diffResolveArrayOrElse(Field goalField, Object srcFieldObj, Object goal) throws IllegalAccessException, NoSuchFieldException, InstantiationException {
        if (null == goalField || null == srcFieldObj) {
            return;
        }
        Class goalCls = goalField.getType();
        String name = goalCls.toString();
        if (isList(goalCls)) {
            resolveGoalList(goalField, srcFieldObj, goal);
        } else if (goalCls.isArray()) {
            resolveGoalArray(goalField, srcFieldObj, goal);
        }  else {
            set(goalField,srcFieldObj,goal);
        }
    }

    /**
     * 判断是不是 List 列表
     *
     * @param cls
     * @return
     */
    private boolean isList(Class cls) {
        String simpleName = cls.getSimpleName();
        return simpleName.equals("List") || simpleName.equals("ArrayList");
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void resolveGoalList(Field goalField, Object srcObj, Object goal) throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        if (goalField == null || srcObj == null || goal == null) {
            return;
        }
        Class goalCls = goalField.getType();
        if (!isList(goalCls)) {
            return;
        }
        Class srcClass = srcObj.getClass();
        Class goalListGenericCls = Utils.getListGeneric(goalField);
        if (goalListGenericCls == null) {
            return;
        }
        List<Object> goalList = new ArrayList<>();

        if (srcClass.isArray()) {
            //TODO srcObj 是数组的情况
            Object[] srcObjs = (Object[]) srcObj;
            Class srcComponentCls = srcClass.getComponentType();
            if (goalListGenericCls.equals(srcComponentCls)) {
                List<Object> list = Utils.arrayToList(srcObjs);
                if (list != null) {
                    goalList.addAll(list);
                }
            } else {
                for (int i = 0; i < srcObjs.length; i++) {
                    Object goalItem = goalListGenericCls.newInstance();
                    resolve(srcObjs[i], goalItem);
                    goalList.add(goalItem);
                }
            }
            goalField.set(goal, goalList);
            return;
        }

        if (isList(srcClass)) {
            //TODO srcObj 是List
            List<Object> srcList = (List<Object>) srcObj;
            if (!Utils.isListEmpty(srcList)) {
                Class srcListGeneric = srcList.get(0).getClass();
                if (srcListGeneric.equals(goalListGenericCls)) {
                    goalList.addAll(srcList);
                } else {
                    for (int i = 0; i < srcList.size(); i++) {
                        Object goalItem = goalListGenericCls.newInstance();
                        resolve(srcList.get(i), goalItem);
                        goalList.add(goalItem);
                    }
                }
                goalField.set(goal, goalList);
            }
        }
    }

    private void set(Field goalField,Object srcObj,Object goal) throws IllegalAccessException {
        if(null == goalField || null == srcObj || null == goal){
            return;
        }
        Class srcCls = srcObj.getClass();
        Class goalCls = goalField.getType();
        if(srcCls.equals(goalCls)){
            goalField.set(goal,srcObj);
            return;
        }
        //判断goal是不是String
        if(Utils.isString(goalCls)){
            String str = Utils.convertToString(srcObj);
            goalField.set(goal,str);
            return;
        }
        //判断goal是不是Long
        if(Utils.isLong(goalCls)){
            String str = Utils.convertToString(srcObj);
            long l = -1;
            try{
                 l = Long.parseLong(str);
            } catch (Exception ignored){
            }
            goalField.set(goal,l);
            return;
        }
        //判断goal是int型
        if(Utils.isInt(goalCls)){
            String str = Utils.convertToString(srcObj);
            int i = -1;
            try{
                i = Integer.parseInt(str);
            } catch (Exception ignored){
            }
            goalField.set(goal,i);
        }
    }
}