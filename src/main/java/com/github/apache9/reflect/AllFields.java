package com.github.apache9.reflect;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author zhangduo
 */
public class AllFields {
    private static Field[] getAllFields(Class<?> clazz) {
        if (clazz == null) {
            return new Field[0];
        }
        List<Field> fieldList = new ArrayList<Field>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field: fields) {
            fieldList.add(field);
        }
        Field[] superFields = getAllFields(clazz.getSuperclass());
        for (Field field: superFields) {
            fieldList.add(field);
        }
        return fieldList.toArray(new Field[0]);
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(getAllFields(B.class)));
    }

}

class A {
    int a;
}

class B extends A {
    long b;
}
