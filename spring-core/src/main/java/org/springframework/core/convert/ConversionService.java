/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.convert;

import org.springframework.lang.Nullable;

/**
 * 一个用于类型转换的服务接口。这是进入转换系统的入口点。
 * 使用{@link #convert(Object, Class)}方法以进行安全的线程类型转换。
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @since 3.0
 */
public interface ConversionService {

	/**
	 * 返回如果可以将{@code sourceType}的对象转换为{@code targetType}，则返回{@code true}。
	 * <p>如果此方法返回{@code true}，则表示{@link #convert(Object, Class)}可以转换{@code sourceType}的实例到{@code targetType}。
	 * <p>特别注意集合，数组和映射类型：
	 * 对于转换集合，数组和映射类型，此方法将返回{@code true}，即使 underlying elements 可能引发{@link ConversionException}。
	 * 调用者需要在处理集合和映射时处理这种异常情况。
	 *
	 * @param sourceType 要从其转换的源类型（如果源为{@code null}，则可能为{@code null}）
	 * @param targetType 要转换到的目标类型（必需）
	 * @return 如果可以执行转换，则返回{@code true}，否则返回{@code false}
	 * @throws IllegalArgumentException 如果{@code targetType}为{@code null}
	 */
	boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType);

	/**
	 * 返回如果可以将{@code sourceType}的对象转换为{@code targetType}，则返回{@code true}。
	 * The TypeDescriptors 提供关于转换发生的源和目标位置的上下文，通常是对象字段或属性位置。
	 * <p>如果此方法返回{@code true}，则表示{@link #convert(Object, TypeDescriptor, TypeDescriptor)}可以转换{@code sourceType}的实例到{@code targetType}。
	 * <p>特别注意集合，数组和映射类型：
	 * 对于转换集合，数组和映射类型，此方法将返回{@code true}，即使 underlying elements 可能引发{@link ConversionException}。
	 * 调用者需要在处理集合和映射时处理这种异常情况。
	 *
	 * @param sourceType 有关要从中转换的源类型的上下文（如果源为{@code null}，则可能为{@code null}）
	 * @param targetType 有关要转换到的目标类型的上下文（必需）
	 * @return 如果可以在源类型和目标类型之间执行转换，则返回{@code true}，否则返回{@code false}
	 * @throws IllegalArgumentException 如果{@code targetType}为{@code null}
	 */
	boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

	/**
	 * 将给定的{@code source}转换为指定的{@code targetType}。
	 *
	 * @param source     要转换的源对象（可能为{@code null}）
	 * @param targetType 要转换到的目标类型（必需）
	 * @return 转换后的对象，{@link TypeDescriptor#getObjectType() targetType}的实例
	 * @throws ConversionException      如果发生转换异常
	 * @throws IllegalArgumentException 如果{@code targetType}为{@code null}
	 */
	@Nullable
	<T> T convert(@Nullable Object source, Class<T> targetType);

	/**
	 * 将给定的{@code source}转换为指定的{@code targetType}。
	 * The TypeDescriptors 提供关于转换发生的源和目标位置的上下文，通常是对象字段或属性位置。
	 *
	 * @param source     要转换的源对象（可能为{@code null}）
	 * @param sourceType 有关要从中转换的源类型的上下文（如果源为{@code null}，则可能为{@code null}）
	 * @param targetType 有关要转换到的目标类型的上下文（必需）
	 * @return 转换后的对象，{@link TypeDescriptor#getObjectType() targetType}的实例
	 * @throws ConversionException      如果发生转换异常
	 * @throws IllegalArgumentException 如果{@code targetType}为{@code null}，
	 *                                  或{@code sourceType}为{@code null}但源不为{@code null}
	 */
	@Nullable
	Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

}
