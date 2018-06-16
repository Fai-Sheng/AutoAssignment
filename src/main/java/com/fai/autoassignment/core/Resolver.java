package com.fai.autoassignment.core;

/**
 * Created by PVer on 2018/6/4.
 */

public interface Resolver {
    <T,K>K execSetParam(T src,K goal);
}
