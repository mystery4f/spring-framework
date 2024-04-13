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

package org.springframework.core.type;

import org.springframework.core.annotation.*;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Defines access to the annotations of a specific type ({@link AnnotationMetadata class}
 * or {@link MethodMetadata method}), in a form that does not necessarily require the
 * class-loading.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @see AnnotationMetadata
 * @see MethodMetadata
 * @since 4.0
 */
public interface AnnotatedTypeMetadata {

	/**
	 * 判断底层元素是否具有给定类型的注释或元注释。
	 * <p>如果此方法返回 {@code true}，则
	 * {@link #getAnnotationAttributes} 将返回一个非空的 Map。
	 *
	 * @param annotationName 要查找的注释类型的完全限定类名
	 * @return 是否定义了匹配的注释
	 */
	default boolean isAnnotated(String annotationName) {
		return getAnnotations().isPresent(annotationName);
	}

	/**
	 * 从底层元素的直接注解中返回注解详细信息。
	 *
	 * @return 基于直接注解的合并注解
	 * @since 5.2
	 */
	MergedAnnotations getAnnotations();

	/**
	 * 检索给定类型注解的属性（如果有的话，即如果在底层元素上定义了直接注解或元注解），
	 * 同时考虑组合注解上的属性覆盖。
	 *
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @return 属性的Map，属性名称作为键（例如"value"），定义的属性值作为Map值。
	 * 如果没有定义匹配的注解，则返回值将为null。
	 */
	@Nullable
	default Map<String, Object> getAnnotationAttributes(String annotationName) {
		return getAnnotationAttributes(annotationName, false);
	}

	/**
	 * 检索给定类型注解的属性（如果有的话，即如果在底层元素上定义了直接注解或元注解），
	 * 同时考虑组合注解上的属性覆盖。
	 *
	 * @param annotationName      要查找的注解类型的完全限定类名
	 * @param classValuesAsString 是否将类引用转换为String类名，以便在返回的Map中作为值公开，
	 *                            而不是可能首先加载的Class引用
	 * @return 属性的Map，属性名称作为键（例如"value"），定义的属性值作为Map值。
	 * 如果没有定义匹配的注解，则返回值将为null。
	 */
	@Nullable
	default Map<String, Object> getAnnotationAttributes(String annotationName,
														boolean classValuesAsString) {

		MergedAnnotation<Annotation> annotation = getAnnotations().get(annotationName,
				null,
				MergedAnnotationSelectors.firstDirectlyDeclared()
		);
		if (!annotation.isPresent()) {
			return null;
		}
		return annotation.asAnnotationAttributes(Adapt.values(classValuesAsString, true));
	}

	/**
	 * 如果有的话（即如果在基础元素上定义为直接注释或元注释），则检索给定类型的所有注释的所有属性。
	 * 请注意，此变体<i>不</i>考虑属性覆盖。
	 *
	 * @param annotationName 要查找的注释类型的完全限定类名
	 * @return 属性的MultiMap，属性名称作为键（例如"value"），定义的属性值列表作为Map值。如果没有定义匹配的注释，则返回值将为{@code null}。
	 * @see #getAllAnnotationAttributes(String, boolean)
	 */
	@Nullable
	default MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName) {
		return getAllAnnotationAttributes(annotationName, false);
	}

	/**
	 * 如果有的话（即如果在基础元素上定义为直接注释或元注释），则检索给定类型的所有注释的所有属性。
	 * 请注意，此变体<i>不</i>考虑属性覆盖。
	 *
	 * @param annotationName      要查找的注释类型的完全限定类名
	 * @param classValuesAsString 是否将类引用转换为字符串
	 * @return 属性的MultiMap，属性名称作为键（例如"value"），定义的属性值列表作为Map值。如果没有定义匹配的注释，则返回值将为{@code null}。
	 * @see #getAllAnnotationAttributes(String)
	 */
	@Nullable
	default MultiValueMap<String, Object> getAllAnnotationAttributes(
			String annotationName, boolean classValuesAsString) {

		Adapt[] adaptations = Adapt.values(classValuesAsString, true);
		return getAnnotations().stream(annotationName)
				.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
				.map(MergedAnnotation::withNonMergedAttributes)
				.collect(MergedAnnotationCollectors.toMultiValueMap(map ->
						map.isEmpty() ? null : map, adaptations));
	}

}
