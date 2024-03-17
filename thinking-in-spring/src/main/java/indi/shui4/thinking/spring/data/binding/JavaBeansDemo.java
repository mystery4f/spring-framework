package indi.shui4.thinking.spring.data.binding;

import indi.shui4.thinking.spring.ioc.overview.domain.User;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.Arrays;

/**
 * @author shui4
 */
public class JavaBeansDemo {
	public static void main(String[] args) throws IntrospectionException {
		// stopClass 排除（截止）类。若不设置 stopClass，将会在 getPropertyDescriptors 中获取到 Object#getClass
		final var beanInfo = Introspector.getBeanInfo(User.class, Object.class);
		final var propertyDescriptors = beanInfo.getPropertyDescriptors();
		System.out.println("propertyDescriptors");
		Arrays.stream(propertyDescriptors).forEach(System.out::println);
		final var methodDescriptors = beanInfo.getMethodDescriptors();
		System.out.println("methodDescriptors");
		Arrays.stream(methodDescriptors).forEach(System.out::println);
	}
}
