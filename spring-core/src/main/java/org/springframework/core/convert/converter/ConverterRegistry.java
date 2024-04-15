/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.convert.converter;

/**
 * 用于向类型转换系统注册转换器的接口。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface ConverterRegistry {

	/**
	 * 向注册表中添加一个普通的转换器。
	 * 转换的源/目标类型对是从转换器的泛型参数类型中派生出来的。
	 *
	 * @throws IllegalArgumentException 如果泛型参数类型无法解析
	 */
	void addConverter(Converter<?, ?> converter);

	/**
	 * 向注册表中添加一个普通的转换器。
	 * 转换的源/目标类型对被显式指定。
	 * <p>允许一个转换器被重用于多个不同的类型对，而无需为每对类型创建一个转换器类。
	 *
	 * @since 3.1
	 */
	<S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Converter<? super S, ? extends T> converter);

	/**
	 * 向注册表中添加一个通用转换器。
	 */
	void addConverter(GenericConverter converter);

	/**
	 * 向注册表中添加一个范围转换器工厂。
	 * 转换的源/目标类型对是从转换器工厂的泛型参数类型中派生出来的。
	 *
	 * @throws IllegalArgumentException 如果泛型参数类型无法解析
	 */
	void addConverterFactory(ConverterFactory<?, ?> factory);

	/**
	 * 从{@code sourceType}到{@code targetType}移除任何转换器。
	 *
	 * @param sourceType 源类型
	 * @param targetType 目标类型
	 */
	void removeConvertible(Class<?> sourceType, Class<?> targetType);

}

