/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Enumeration;

/**
 * Common delegate methods for Spring's internal {@link PropertyDescriptor} implementations.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
abstract class PropertyDescriptorUtils {

	/**
	 * 复制属性描述符中的非方法属性到目标属性描述符。
	 * 这个是JavaBean功能的一部分，用来在Bean类中处理属性的元数据。
	 *
	 * @param source 源属性描述符
	 * @param target 目标属性描述符
	 */
	public static void copyNonMethodProperties(PropertyDescriptor source, PropertyDescriptor target) {
		// 将源属性描述符的专家模式标记复制到目标属性描述符
		target.setExpert(source.isExpert());
		// 将源属性描述符的隐藏属性复制到目标属性描述符
		target.setHidden(source.isHidden());
		// 将源属性描述符的优先属性复制到目标属性描述符
		target.setPreferred(source.isPreferred());
		// 将源属性描述符的名称复制到目标属性描述符
		target.setName(source.getName());
		// 将源属性描述符的简短描述复制到目标属性描述符
		target.setShortDescription(source.getShortDescription());
		// 将源属性描述符的显示名称复制到目标属性描述符
		target.setDisplayName(source.getDisplayName());

		// 复制所有的自定义属性键值对
		Enumeration<String> keys = source.attributeNames();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			// 设置目标属性描述符的自定义属性值
			target.setValue(key, source.getValue(key));
		}

		// 复制属性编辑器类、是否bounds和是否constrained的标志
		target.setPropertyEditorClass(source.getPropertyEditorClass());
		target.setBound(source.isBound());
		target.setConstrained(source.isConstrained());
	}

	/**
	 * 查找属性的类型。使用读方法和写方法的签名来确定属性的Java类型。
	 *
	 * @param readMethod  读方法
	 * @param writeMethod 写方法
	 * @return 返回属性的类型，如果无法determined则返回null
	 * @throws IntrospectionException 如果存在属性类型不匹配或者方法签名不正确则抛出异常
	 */
	@Nullable
	public static Class<?> findPropertyType(@Nullable Method readMethod, @Nullable Method writeMethod) throws IntrospectionException {

		Class<?> propertyType = null;

		// 如果存在读方法，确定方法返回类型
		if (readMethod != null) {
			// 确认读方法无参数
			if (readMethod.getParameterCount() != 0) {
				throw new IntrospectionException("Bad read method arg count: " + readMethod);
			}
			// 获取读方法返回类型
			propertyType = readMethod.getReturnType();
			// 如果返回类型为void，则抛出异常
			if (propertyType == Void.TYPE) {
				throw new IntrospectionException("Read method returns void: " + readMethod);
			}
		}

		// 如果存在写方法，确定方法参数类型
		if (writeMethod != null) {
			Class<?>[] params = writeMethod.getParameterTypes();
			// 确认写方法只有一个参数
			if (params.length != 1) {
				throw new IntrospectionException("Bad write method arg count: " + writeMethod);
			}
			// 如果已经确定读方法的属性类型，验证写方法参数类型是否兼容
			if (propertyType != null) {
				// 如果写方法参数类型可以分配给读方法的返回类型或反过来，使用该类型
				if (!propertyType.isAssignableFrom(params[0]) && !params[0].isAssignableFrom(propertyType)) {
					throw new IntrospectionException("Type mismatch between read and write methods: " + readMethod + " - " + writeMethod);
				}
			} else {
				// 如果没有读方法，使用写方法的参数类型
				propertyType = params[0];
			}
		}

		// 返回确定的属性类型
		return propertyType;
	}

	/**
	 * 查找具有索引的属性的类型，这是JavaBean特性中的一个高级特性，允许通过索引访问属性的一个元素。
	 *
	 * @param name               属性的名字
	 * @param propertyType       属性的类型
	 * @param indexedReadMethod  具有索引的读方法
	 * @param indexedWriteMethod 具有索引的写方法
	 * @return 返回索引属性的类型，如果无法确定则返回null
	 * @throws IntrospectionException 如果存在类型不匹配或者方法签名不正确则抛出异常
	 */
	@Nullable
	public static Class<?> findIndexedPropertyType(String name, @Nullable Class<?> propertyType, @Nullable Method indexedReadMethod, @Nullable Method indexedWriteMethod) throws IntrospectionException {

		Class<?> indexedPropertyType = null;

		// 如果存在具有索引的读方法，确定方法返回类型
		if (indexedReadMethod != null) {
			Class<?>[] params = indexedReadMethod.getParameterTypes();
			// 确认具有索引的读方法只有一个int类型的参数
			if (params.length != 1 || params[0] != Integer.TYPE) {
				throw new IntrospectionException("Bad indexed read method args: " + indexedReadMethod);
			}
			// 获取具有索引的读方法返回类型
			indexedPropertyType = indexedReadMethod.getReturnType();
			// 如果返回类型为void，则抛出异常
			if (indexedPropertyType == Void.TYPE) {
				throw new IntrospectionException("Indexed read method returns void: " + indexedReadMethod);
			}
		}

		// 如果存在具有索引的写方法，确定方法第二个参数的类型，第一个参数必须为int类型
		if (indexedWriteMethod != null) {
			Class<?>[] params = indexedWriteMethod.getParameterTypes();
			// 确认具有索引的写方法有两个参数，且第一个参数为int类型
			if (params.length != 2 || params[0] != Integer.TYPE) {
				throw new IntrospectionException("Bad indexed write method args: " + indexedWriteMethod);
			}
			// 如果已经确定具有索引的读方法的返回类型，验证写方法的第二个参数类型是否兼容
			if (indexedPropertyType != null && !indexedPropertyType.isAssignableFrom(params[1]) && !params[1].isAssignableFrom(
					indexedPropertyType)) {
				throw new IntrospectionException("Type mismatch between indexed read and write methods: " + indexedReadMethod + " - " + indexedWriteMethod);
			} else {
				// 如果没有具有索引的读方法或类型兼容，使用写方法的第二个参数类型
				indexedPropertyType = params[1];
			}
		}

		// 验证具有索引的属性类型与普通属性类型是否兼容
		if (propertyType != null && (!propertyType.isArray() || propertyType.getComponentType() != indexedPropertyType)) {
			throw new IntrospectionException("Type mismatch between indexed and non-indexed methods: " + indexedReadMethod + " - " + indexedWriteMethod);
		}

		// 返回确定的具有索引的属性类型
		return indexedPropertyType;
	}

	/**
	 * 比较给定的{@code PropertyDescriptors}并返回{@code true}如果它们是等价的。
	 * 即它们的读方法、写方法、属性类型、属性编辑器和标志是等效的。
	 * 此比较基于java.beans.PropertyDescriptor的equals方法。
	 *
	 * @param pd      属性描述符1
	 * @param otherPd 属性描述符2
	 * @return 如果两个属性描述符等价返回true，否则返回false
	 */
	public static boolean equals(PropertyDescriptor pd, PropertyDescriptor otherPd) {
		// 比较读方法、写方法、属性类型、属性编辑器类、是否bound和是否constrained
		return (ObjectUtils.nullSafeEquals(
				pd.getReadMethod(),
				otherPd.getReadMethod()
		) && ObjectUtils.nullSafeEquals(
				pd.getWriteMethod(),
				otherPd.getWriteMethod()
		) && ObjectUtils.nullSafeEquals(pd.getPropertyType(), otherPd.getPropertyType()) && ObjectUtils.nullSafeEquals(
				pd.getPropertyEditorClass(),
				otherPd.getPropertyEditorClass()
		) && pd.isBound() == otherPd.isBound() && pd.isConstrained() == otherPd.isConstrained());
	}


}
