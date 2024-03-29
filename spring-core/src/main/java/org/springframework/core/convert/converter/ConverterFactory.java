/*
 * Copyright 2002-2018 the original author or authors.
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
 * 一个用于创建“范围”转换器的工厂，可以将对象从S转换为R的子类型。
 *
 * <p>实现还可以实现{@link ConditionalConverter}。
 *
 * @param <S> 工厂创建的转换器可以从此源类型转换
 * @param <R> 工厂创建的转换器可以转换为的目标范围（或基本）类型；例如，{@link Number} 用于一组数字子类型。
 * @author Keith Donald
 * @see ConditionalConverter
 * @since 3.0
 */
public interface ConverterFactory<S, R> {

	/**
	 * 获取从S转换为目标类型T的转换器，其中T也是R的实例。
	 *
	 * @param <T>        目标类型
	 * @param targetType 要转换为的目标类型
	 * @return 从S到T的转换器
	 */
	<T extends R> Converter<S, T> getConverter(Class<T> targetType);

}
