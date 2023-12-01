/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.function.Function;

/**
 * 定义了一个通用的接口，用于向任意对象附加和访问元数据。
 *
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.0
 */
public interface AttributeAccessor {

	/**
	 * 将由 {@code name} 定义的属性设置为提供的 {@code value}。
	 * <p>如果 {@code value} 为 {@code null}，则会 {@link #removeAttribute 删除} 属性。
	 * <p>一般来说，用户应该通过使用全限定名来防止与其他元数据属性重叠，可能使用类或包名作为前缀。
	 *
	 * @param name  唯一的属性键
	 * @param value 要附加的属性值
	 */
	void setAttribute(String name, @Nullable Object value);

	/**
	 * 获取由 {@code name} 标识的属性的值。
	 * <p>如果属性不存在，则返回 {@code null}。
	 *
	 * @param name 唯一的属性键
	 * @return 属性的当前值（如果有的话）
	 */
	@Nullable
	Object getAttribute(String name);

	/**
	 * 如果需要，计算由 {@code name} 标识的属性的新值，并在此 {@code AttributeAccessor} 中 {@linkplain #setAttribute 设置} 新值。
	 * <p>如果此 {@code AttributeAccessor} 中已经存在由 {@code name} 标识的属性的值，则返回现有值，而不应用提供的计算函数。
	 * <p>此方法的默认实现不是线程安全的，但可以被此接口的具体实现重写。
	 *
	 * @param <T>             属性值的类型
	 * @param name            唯一的属性键
	 * @param computeFunction 用于计算属性新值的函数；该函数不得返回 {@code null} 值
	 * @return 名称属性的现有值或新计算的值
	 * @see #getAttribute(String)
	 * @see #setAttribute(String, Object)
	 * @since 5.3.3
	 */
	@SuppressWarnings("unchecked")
	default <T> T computeAttribute(String name, Function<String, T> computeFunction) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(computeFunction, "Compute function must not be null");
		Object value = getAttribute(name);
		if (value == null) {
			value = computeFunction.apply(name);
			Assert.state(value != null,
					() -> String.format("Compute function must not return null for attribute named '%s'", name));
			setAttribute(name, value);
		}
		return (T) value;
	}

	/**
	 * 移除由 {@code name} 标识的属性并返回其值。
	 * <p>如果找不到 {@code name} 下的属性，则返回 {@code null}。
	 *
	 * @param name 唯一的属性键
	 * @return 属性的最后一个值（如果有的话）
	 */
	@Nullable
	Object removeAttribute(String name);

	/**
	 * 如果由 {@code name} 标识的属性存在，则返回 {@code true}。
	 * <p>否则返回 {@code false}。
	 *
	 * @param name 唯一的属性键
	 */
	boolean hasAttribute(String name);

	/**
	 * 返回所有属性的名称。
	 */
	String[] attributeNames();

}
