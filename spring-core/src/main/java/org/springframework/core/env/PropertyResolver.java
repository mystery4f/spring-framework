/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.env;

import org.springframework.lang.Nullable;

/**
 * 用于针对任何基础源解析属性的接口。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see Environment
 * @see PropertySourcesPropertyResolver
 * @since 3.1
 */
public interface PropertyResolver {

	/**
	 * 确定给定的属性键是否可以解析，即给定键对应的值是否不为{@code null}。
	 */
	boolean containsProperty(String key);

	/**
	 * 返回与给定键关联的属性值，如果无法解析键，则返回 {@code null}。
	 *
	 * @param key 要解析的属性名称
	 * @see #getProperty(String, String)
	 * @see #getProperty(String, Class)
	 * @see #getRequiredProperty(String)
	 */
	@Nullable
	String getProperty(String key);

	/**
	 * 返回与给定键关联的属性值，如果无法解析键则返回 {@code defaultValue}。
	 *
	 * @param key          要解析的属性名称
	 * @param defaultValue 如果找不到值，要返回的默认值
	 * @see #getRequiredProperty(String)
	 * @see #getProperty(String, Class)
	 */
	String getProperty(String key, String defaultValue);

	/**
	 * 返回与给定键关联的属性值，如果无法解析键则返回 {@code null}。
	 *
	 * @param key        要解析的属性名称
	 * @param targetType 属性值的期望类型
	 * @see #getRequiredProperty(String, Class)
	 */
	@Nullable
	<T> T getProperty(String key, Class<T> targetType);

	/**
	 * 返回与给定键关联的属性值，如果无法解析键则返回 {@code defaultValue}。
	 *
	 * @param key          要解析的属性名称
	 * @param targetType   属性值的期望类型
	 * @param defaultValue 如果找不到值，要返回的默认值
	 * @see #getRequiredProperty(String, Class)
	 */
	<T> T getProperty(String key, Class<T> targetType, T defaultValue);

	/**
	 * 返回与给定键关联的属性值（不为{@code null}）。
	 *
	 * @throws IllegalStateException 如果无法解析键
	 * @see #getRequiredProperty(String, Class)
	 */
	String getRequiredProperty(String key) throws IllegalStateException;

	/**
	 * 返回与给定键关联的属性值，转换为给定的 {@code targetType}（不为{@code null}）。
	 *
	 * @throws IllegalStateException 如果无法解析给定的键
	 */
	<T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

	/**
	 * 解析给定文本中的 ${...} 占位符，将其替换为由 {@link #getProperty} 解析的相应属性值。对于
	 * 无法解析的没有默认值的占位符，将被忽略并原样传递。
	 *
	 * @param text 要解析的字符串
	 * @return 解析后的字符串（不为{@code null}）
	 * @throws IllegalArgumentException 如果给定的文本为{@code null}
	 * @see #resolveRequiredPlaceholders
	 */
	String resolvePlaceholders(String text);

	/**
	 * 解析给定文本中的 ${...} 占位符，将其替换为由 {@link #getProperty} 解析的相应属性值。对于
	 * 无法解析的没有默认值的占位符，将抛出 IllegalArgumentException 异常。
	 *
	 * @return 解析后的字符串（不为{@code null}）
	 * @throws IllegalArgumentException 如果给定的文本为{@code null}或有任何无法解析的占位符
	 */
	String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;

}
