package indi.shui4.thinking.spring.tool;

import cn.hutool.core.util.ReflectUtil;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 泛型工具 {@link ResolvableType} 实例
 *
 * @author shui4
 */
public class ResolvableTypeExample {


    public static class MyArrayList extends ArrayList<Integer> {


        private Map<String, Map<String,Integer>> map;


        public void add(String key, Map<String, Integer> value) {

        }
    }


    public static void main(String[] args) {
        // 获取类的参数
        System.out.println("类");
        ResolvableType resolvableType = ResolvableType.forClass(MyArrayList.class).as(List.class);
        System.out.println(resolvableType.getGeneric());

        System.out.println("字段");
        Field field = ReflectUtil.getField(MyArrayList.class, "map");
        ResolvableType fieldResolvableType = ResolvableType.forField(field);
        System.out.println(fieldResolvableType.getType());
        ParameterizedType genericType = (ParameterizedType) field.getGenericType();
        System.out.println("getGenericType:"+ genericType.getRawType());
        System.out.println(genericType.getActualTypeArguments());
        System.out.println(fieldResolvableType.resolve());
        System.out.println(fieldResolvableType.getGeneric(0));
        System.out.println(fieldResolvableType.getGeneric(1));
        System.out.println(fieldResolvableType.getGeneric(1).getGeneric(0));
        System.out.println(fieldResolvableType.getGeneric(1).getGeneric(1));

        System.out.println("方法参数");
        ResolvableType methodParameterResolvableType = ResolvableType.forMethodParameter(ReflectUtil.getMethod(MyArrayList.class, "add",String.class,Map.class), 0);
        ResolvableType methodParameterResolvableType1 = ResolvableType.forMethodParameter(ReflectUtil.getMethod(MyArrayList.class, "add",String.class,Map.class), 1);
        System.out.println(methodParameterResolvableType);
        System.out.println(methodParameterResolvableType1);

    }
}
