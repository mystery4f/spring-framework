package indi.shui4.thinking.spring.generic;

import cn.hutool.core.lang.Console;
import org.springframework.core.GenericTypeResolver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.core.GenericTypeResolver.resolveReturnType;
import static org.springframework.core.GenericTypeResolver.resolveReturnTypeArgument;

/**
 * {@link GenericTypeResolver}示例
 *
 * @author shui4
 */
@SuppressWarnings("ALL")
public class GenericTypeResolverDemo {
	public static void main(String[] args) throws NoSuchMethodException {
		displayReturnTypeGenericInfo("getString", String.class);
		displayReturnTypeGenericInfo("getString", Comparable.class);
		displayReturnTypeGenericInfo("getList", List.class);
		displayReturnTypeGenericInfo("getStringList", List.class);
		final var typeVariableMap = GenericTypeResolver.getTypeVariableMap(StringList.class);
		Console.log("typeVariableMap={}", typeVariableMap);
	}

	private static void displayReturnTypeGenericInfo(String methodName, Class<?> genericIfc) {
		final Method method;
		try {
			method = GenericTypeResolverDemo.class.getMethod(methodName);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		Console.log("methodName={},resolveReturnType={}",
				methodName,
				resolveReturnType(method, GenericTypeResolverDemo.class)

		);
		Console.log("methodName={},resolveReturnTypeArgument={},genericIfc={}\n",
				methodName,
				resolveReturnTypeArgument(method, genericIfc),
				genericIfc.getSimpleName()
		);

	}

	public static <E> ArrayList<E> getList() {
		return null;
	}

	public static <E> StringList getStringList() {
		return null;
	}

	public static String getString() {
		return null;
	}

}
