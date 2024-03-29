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

package org.springframework.core.convert.converter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A converter converts a source object of type {@code S} to a target of type {@code T}.
 *
 * <p>Implementations of this interface are thread-safe and can be shared.
 *
 * <p>Implementations may additionally implement {@link ConditionalConverter}.
 *
 * @param <S> the source type
 * @param <T> the target type
 * @author Keith Donald
 * @author Josh Cummings
 * @since 3.0
 */
@FunctionalInterface
public interface Converter<S, T> {

	/**
	 * 构造一个组合的{@link Converter}，首先应用此{@link Converter}
	 * 到其输入，然后应用{@code after} {@link Converter}到结果。
	 *
	 * @param after 在应用此{@link Converter}后应用的{@link Converter}
	 * @param <U>   both the {@code after} {@link Converter} and the composed {@link Converter}的输出类型
	 * @return 一个组合的{@link Converter}，首先应用此{@link Converter}
	 * 然后应用{@code after} {@link Converter}
	 * @since 5.3
	 */
	default <U> Converter<S, U> andThen(Converter<? super T, ? extends U> after) {
		Assert.notNull(after, "After Converter must not be null");
		return (S s) -> {
			T initialResult = convert(s);
			return (initialResult != null ? after.convert(initialResult) : null);
		};
	}

	/**
	 * 将类型为 {@code S} 的源对象转换为目标类型 {@code T}。
	 *
	 * @param source 要转换的源对象，必须是 {@code S} 的实例（绝不能为 {@code null})
	 * @return 转换后的对象，必须是 {@code T} 的实例（ potentially {@code null})
	 * @throws IllegalArgumentException 如果源对象不能转换为所需的目标类型
	 */
	@Nullable
	T convert(S source);

}
