/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.core.convert.TypeDescriptor;

/**
 * 允许{@link Converter}, {@link GenericConverter}或{@link ConverterFactory}根据{@code source}和{@code target}
 * {@link TypeDescriptor}的属性条件性地执行。
 *
 * <p>常常用于选择性地匹配基于字段或类级特征的自定义转换逻辑，例如注解或方法。例如，在将String字段转换为Date字段时，
 * 实现可能会返回{@code true}，如果目标字段也被annotated with {@code @DateTimeFormat}。
 *
 * <p>作为另一个例子，在将String字段转换为{@code Account}字段时，实现可能会返回{@code true}，如果目标Account类
 * 定义了一个{@code public static findAccount(String)}方法。
 *
 * @author Phillip Webb
 * @author Keith Donald
 * @see Converter
 * @see GenericConverter
 * @see ConverterFactory
 * @see ConditionalGenericConverter
 * @since 3.2
 */
public interface ConditionalConverter {

	/**
	 * 当前考虑的从{@code sourceType}转换为{@code targetType}的转换应该被选择吗？
	 *
	 * @param sourceType 我们正在转换的从其字段的类型描述符
	 * @param targetType 我们正在转换的目标其字段的类型描述符
	 * @return 如果应该执行转换，则为true，否则为false
	 */
	boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);

}
