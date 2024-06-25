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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

/**
 * {@link MergedAnnotationSelector} implementations that provide various options
 * for {@link MergedAnnotation} instances.
 *
 * @author Phillip Webb
 * @see MergedAnnotations#get(Class, Predicate, MergedAnnotationSelector)
 * @see MergedAnnotations#get(String, Predicate, MergedAnnotationSelector)
 * @since 5.2
 */
public abstract class MergedAnnotationSelectors {

	private static final MergedAnnotationSelector<?> NEAREST = new Nearest();

	private static final MergedAnnotationSelector<?> FIRST_DIRECTLY_DECLARED = new FirstDirectlyDeclared();


	private MergedAnnotationSelectors() {
	}


	/**
	 * 选择最近的注解，即具有最低距离的注解。
	 * <p>
	 * 此方法旨在提供一种简单的方法来获取选取器实例，该实例优先选择注解层级中距离最近的注解。
	 * 当处理多层继承或覆盖的注解时，此功能尤为有用，应用程序希望优先选择与目标最直接相关的注解。
	 *
	 * @param <A> 类型参数，表示要被选择的注解类型。
	 * @return 返回一个优先选取最近注解的选取器实例。
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> MergedAnnotationSelector<A> nearest() {
		return (MergedAnnotationSelector<A>) NEAREST;
	}

	/**
	 * 尽可能选择首个直接声明的注解。如果没有直接声明的注解，则选择最近的注解。
	 * <p>
	 * 此方法旨在提供一种方式，专门用于选择直接在目标元素上声明的注解，而非从超类或接口继承的注解。
	 * 在处理注解继承时，当应用程序希望明确使用定义在目标元素上的注解，而非继承来的注解时，此功能尤为重要。
	 *
	 * @param <A> 类型参数，表示要被选择的注解类型。
	 * @return 返回一个优先选取首个直接声明注解的选取器实例。
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> MergedAnnotationSelector<A> firstDirectlyDeclared() {
		return (MergedAnnotationSelector<A>) FIRST_DIRECTLY_DECLARED;
	}


	/**
	 * {@link MergedAnnotationSelector} to select the nearest annotation.
	 */
	private static class Nearest implements MergedAnnotationSelector<Annotation> {

		@Override
		public boolean isBestCandidate(MergedAnnotation<Annotation> annotation) {
			return annotation.getDistance() == 0;
		}

		@Override
		public MergedAnnotation<Annotation> select(
				MergedAnnotation<Annotation> existing, MergedAnnotation<Annotation> candidate) {

			if (candidate.getDistance() < existing.getDistance()) {
				return candidate;
			}
			return existing;
		}

	}


	/**
	 * {@link MergedAnnotationSelector} to select the first directly declared
	 * annotation.
	 */
	private static class FirstDirectlyDeclared implements MergedAnnotationSelector<Annotation> {

		@Override
		public boolean isBestCandidate(MergedAnnotation<Annotation> annotation) {
			return annotation.getDistance() == 0;
		}

		@Override
		public MergedAnnotation<Annotation> select(
				MergedAnnotation<Annotation> existing, MergedAnnotation<Annotation> candidate) {

			if (existing.getDistance() > 0 && candidate.getDistance() == 0) {
				return candidate;
			}
			return existing;
		}

	}

}
