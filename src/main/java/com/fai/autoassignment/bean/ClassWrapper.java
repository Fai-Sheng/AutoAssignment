package com.fai.autoassignment.bean;

import java.util.HashMap;

/**
 * Created by PVer on 2018/7/8.
 */

public class ClassWrapper {

    public int classrRank;  //当前的层级
    public Object value;
    public HashMap<String,Object> valueMap;
    public boolean isOrigin;  //是不是系统提供的类 基本类型
    public Object parent;

}
