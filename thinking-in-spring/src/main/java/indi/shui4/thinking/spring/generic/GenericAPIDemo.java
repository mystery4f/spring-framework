package indi.shui4.thinking.spring.generic;

import cn.hutool.core.lang.Console;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * @author shui4
 */
public class GenericAPIDemo {
	public static void main(String[] args) {
		final var intClass = int.class;
		final var objectArrayClass = Object[].class;
		final var rawClass = String.class;
		final var parameterizedType = (ParameterizedType) ArrayList.class.getGenericSuperclass();
		Console.log("parameterizedType={}", parameterizedType);
		Console.log("parameterizedType.getRawType()={}", parameterizedType.getRawType());
		Stream.of(parameterizedType.getActualTypeArguments())
				.map(TypeVariable.class::cast)
				.forEach(type -> Console.log("parameterizedType.getActualTypeArguments() forEach item={}", type));
	}
}
