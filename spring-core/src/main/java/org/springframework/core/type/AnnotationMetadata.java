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

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 定义抽象访问特定类注释的接口，在不加载该类的情况下即可访问。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Phillip Webb
 * @author Sam Brannen
 * @see StandardAnnotationMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getAnnotationMetadata()
 * @see AnnotatedTypeMetadata
 * @since 2.5
 */

public interface AnnotationMetadata extends ClassMetadata, AnnotatedTypeMetadata {

	/**
	 * 通过标准反射创建一个新的 {@link AnnotationMetadata} 实例的工厂方法，用于给定类的内省。
	 *
	 * @param type 要内省的类
	 * @return 一个新的 {@link AnnotationMetadata} 实例
	 * @since 5.2
	 */
	static AnnotationMetadata introspect(Class<?> type) {
		return StandardAnnotationMetadata.from(type);
	}

	/**
	 * 获取基础类上存在的所有注解类型的完全限定类名。
	 *
	 * @return 注解类型的名称集合
	 */
	default Set<String> getAnnotationTypes() {
		return getAnnotations()
				.stream()
				.filter(MergedAnnotation::isDirectlyPresent)
				.map(annotation -> annotation
						.getType()
						.getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 获取基础类上给定注解类型存在的所有元注解类型的完全限定类名。
	 *
	 * @param annotationName 要查找的元注解的完全限定类名
	 * @return 元注解类型的名称集合，如果没有找到则返回一个空集
	 */
	default Set<String> getMetaAnnotationTypes(String annotationName) {
		MergedAnnotation<?> annotation = getAnnotations().get(annotationName,
				MergedAnnotation::isDirectlyPresent
		);
		if (!annotation.isPresent()) {
			return Collections.emptySet();
		}
		return MergedAnnotations
				.from(annotation.getType(),
						SearchStrategy.INHERITED_ANNOTATIONS
				)
				.stream()
				.map(mergedAnnotation -> mergedAnnotation
						.getType()
						.getName())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 确定给定类型的注解是否存在于基础类上。
	 *
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @return 如果存在匹配的注解，则返回 {@code true}
	 */
	default boolean hasAnnotation(String annotationName) {
		return getAnnotations().isDirectlyPresent(annotationName);
	}

	/**
	 * 确定基础类是否具有其本身带有给定类型的元注解的注解。
	 *
	 * @param metaAnnotationName 要查找的元注解类型的完全限定类名
	 * @return 如果存在匹配的元注解，则返回 {@code true}
	 */
	default boolean hasMetaAnnotation(String metaAnnotationName) {
		return getAnnotations()
				.get(metaAnnotationName,
						MergedAnnotation::isMetaPresent
				)
				.isPresent();
	}

	/**
	 * 确定底层类是否有任何方法被给定的注解类型（或元注解）注解。
	 *
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @return 如果有被注解的方法则返回 true，否则返回 false
	 */
	default boolean hasAnnotatedMethods(String annotationName) {
		return !getAnnotatedMethods(annotationName).isEmpty();
	}

	/**
	 * 检索所有被注解（或元注解）标记的方法的方法元数据。
	 * <p>对于任何返回的方法，{@link MethodMetadata#isAnnotated} 将对给定的注解类型返回 {@code true}。
	 *
	 * @param annotationName 要查找的注解类型的完全限定类名
	 * @return 一个包含匹配注解的方法的 {@link MethodMetadata} 集合。如果没有方法匹配注解类型，则返回值将是一个空集。
	 */
	Set<MethodMetadata> getAnnotatedMethods(String annotationName);


}
