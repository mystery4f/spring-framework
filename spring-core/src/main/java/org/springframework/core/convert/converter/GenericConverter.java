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

package org.springframework.core.convert.converter;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Set;

/**
 * 通用转换器接口，用于在两种或多种类型之间进行转换。
 *
 * <p>这是Converter SPI接口中最灵活的，但也是最复杂的。它是灵活的，因为GenericConverter可以支持在多个源/目标类型对之间进行转换（参见{@link #getConvertibleTypes()}）。
 * 此外，GenericConverter实现在类型转换过程中可以访问源/目标{@link TypeDescriptor 字段上下文}。这允许解析源和目标字段元数据，如注解和泛型信息，这些信息可以用于影响转换逻辑。
 *
 * <p>当简单的{@link Converter}或{@link ConverterFactory}接口足够时，通常不应使用此接口。
 *
 * <p>实现还可以实现{@link ConditionalConverter}。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @see TypeDescriptor
 * @see Converter
 * @see ConverterFactory
 * @see ConditionalConverter
 * @since 3.0
 */
public interface GenericConverter {

	/**
	 * 返回此转换器可以转换的源类型和目标类型。
	 * <p>每个条目都是可转换的源-目标类型对。
	 * <p>对于{@link ConditionalConverter 条件转换器}，此方法可能返回
	 * {@code null}以表示所有源-目标对都应该被考虑。
	 */
	@Nullable
	Set<ConvertiblePair> getConvertibleTypes();

	/**
	 * 将源对象转换为{@code TypeDescriptor}描述的目标类型。
	 *
	 * @param source     要转换的源对象（可能为{@code null}）
	 * @param sourceType 转换前的类型描述符
	 * @param targetType 转换后的类型描述符
	 * @return 转换后的对象
	 */
	@Nullable
	Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType);


	/**
	 * Holder for a source-to-target class pair.
	 */
	final class ConvertiblePair {

		private final Class<?> sourceType;

		private final Class<?> targetType;

		/**
		 * Create a new source-to-target pair.
		 *
		 * @param sourceType the source type
		 * @param targetType the target type
		 */
		public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
			Assert.notNull(sourceType, "Source type must not be null");
			Assert.notNull(targetType, "Target type must not be null");
			this.sourceType = sourceType;
			this.targetType = targetType;
		}

		public Class<?> getSourceType() {
			return this.sourceType;
		}

		public Class<?> getTargetType() {
			return this.targetType;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || other.getClass() != ConvertiblePair.class) {
				return false;
			}
			ConvertiblePair otherPair = (ConvertiblePair) other;
			return (this.sourceType == otherPair.sourceType && this.targetType == otherPair.targetType);
		}

		@Override
		public int hashCode() {
			return (this.sourceType.hashCode() * 31 + this.targetType.hashCode());
		}

		@Override
		public String toString() {
			return (this.sourceType.getName() + " -> " + this.targetType.getName());
		}
	}

}
