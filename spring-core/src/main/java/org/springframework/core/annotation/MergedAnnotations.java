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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Provides access to a collection of merged annotations, usually obtained
 * from a source such as a {@link Class} or {@link Method}.
 *
 * <p>Each merged annotation represents a view where the attribute values may be
 * "merged" from different source values, typically:
 *
 * <ul>
 * <li>Explicit and Implicit {@link AliasFor @AliasFor} declarations on one or
 * more attributes within the annotation</li>
 * <li>Explicit {@link AliasFor @AliasFor} declarations for a meta-annotation</li>
 * <li>Convention based attribute aliases for a meta-annotation</li>
 * <li>From a meta-annotation declaration</li>
 * </ul>
 *
 * <p>For example, a {@code @PostMapping} annotation might be defined as follows:
 *
 * <pre class="code">
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;RequestMapping(method = RequestMethod.POST)
 * public &#064;interface PostMapping {
 *
 *     &#064;AliasFor(attribute = "path")
 *     String[] value() default {};
 *
 *     &#064;AliasFor(attribute = "value")
 *     String[] path() default {};
 * }
 * </pre>
 *
 * <p>If a method is annotated with {@code @PostMapping("/home")} it will contain
 * merged annotations for both {@code @PostMapping} and the meta-annotation
 * {@code @RequestMapping}. The merged view of the {@code @RequestMapping}
 * annotation will contain the following attributes:
 *
 * <p><table border="1">
 * <tr>
 * <th>Name</th>
 * <th>Value</th>
 * <th>Source</th>
 * </tr>
 * <tr>
 * <td>value</td>
 * <td>"/home"</td>
 * <td>Declared in {@code @PostMapping}</td>
 * </tr>
 * <tr>
 * <td>path</td>
 * <td>"/home"</td>
 * <td>Explicit {@code @AliasFor}</td>
 * </tr>
 * <tr>
 * <td>method</td>
 * <td>RequestMethod.POST</td>
 * <td>Declared in meta-annotation</td>
 * </tr>
 * </table>
 *
 * <p>{@link MergedAnnotations} can be obtained {@linkplain #from(AnnotatedElement)
 * from} any Java {@link AnnotatedElement}. They may also be used for sources that
 * don't use reflection (such as those that directly parse bytecode).
 *
 * <p>Different {@linkplain SearchStrategy search strategies} can be used to locate
 * related source elements that contain the annotations to be aggregated. For
 * example, {@link SearchStrategy#TYPE_HIERARCHY} will search both superclasses and
 * implemented interfaces.
 *
 * <p>From a {@link MergedAnnotations} instance you can either
 * {@linkplain #get(String) get} a single annotation, or {@linkplain #stream()
 * stream all annotations} or just those that match {@linkplain #stream(String)
 * a specific type}. You can also quickly tell if an annotation
 * {@linkplain #isPresent(String) is present}.
 *
 * <p>Here are some typical examples:
 *
 * <pre class="code">
 * // is an annotation present or meta-present?
 * mergedAnnotations.isPresent(ExampleAnnotation.class);
 *
 * // get the merged "value" attribute of ExampleAnnotation (either directly or
 * // meta-present)
 * mergedAnnotations.get(ExampleAnnotation.class).getString("value");
 *
 * // get all meta-annotations but no directly present annotations
 * mergedAnnotations.stream().filter(MergedAnnotation::isMetaPresent);
 *
 * // get all ExampleAnnotation declarations (including any meta-annotations) and
 * // print the merged "value" attributes
 * mergedAnnotations.stream(ExampleAnnotation.class)
 *     .map(mergedAnnotation -&gt; mergedAnnotation.getString("value"))
 *     .forEach(System.out::println);
 * </pre>
 *
 * <p><b>NOTE: The {@code MergedAnnotations} API and its underlying model have
 * been designed for composable annotations in Spring's common component model,
 * with a focus on attribute aliasing and meta-annotation relationships.</b>
 * There is no support for retrieving plain Java annotations with this API;
 * please use standard Java reflection or Spring's {@link AnnotationUtils}
 * for simple annotation retrieval purposes.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @see MergedAnnotation
 * @see MergedAnnotationCollectors
 * @see MergedAnnotationPredicates
 * @see MergedAnnotationSelectors
 * @since 5.2
 */
public interface MergedAnnotations extends Iterable<MergedAnnotation<Annotation>> {

	/**
	 * Determine if the specified annotation is either directly present or
	 * meta-present.
	 * <p>Equivalent to calling {@code get(annotationType).isPresent()}.
	 *
	 * @param annotationType the annotation type to check
	 * @return {@code true} if the annotation is present
	 */
	<A extends Annotation> boolean isPresent(Class<A> annotationType);

	/**
	 * Determine if the specified annotation is either directly present or
	 * meta-present.
	 * <p>Equivalent to calling {@code get(annotationType).isPresent()}.
	 *
	 * @param annotationType the fully qualified class name of the annotation type
	 *                       to check
	 * @return {@code true} if the annotation is present
	 */
	boolean isPresent(String annotationType);

	/**
	 * Determine if the specified annotation is directly present.
	 * <p>Equivalent to calling {@code get(annotationType).isDirectlyPresent()}.
	 *
	 * @param annotationType the annotation type to check
	 * @return {@code true} if the annotation is directly present
	 */
	<A extends Annotation> boolean isDirectlyPresent(Class<A> annotationType);

	/**
	 * Determine if the specified annotation is directly present.
	 * <p>Equivalent to calling {@code get(annotationType).isDirectlyPresent()}.
	 *
	 * @param annotationType the fully qualified class name of the annotation type
	 *                       to check
	 * @return {@code true} if the annotation is directly present
	 */
	boolean isDirectlyPresent(String annotationType);

	/**
	 * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
	 * annotation or meta-annotation of the specified type, or
	 * {@link MergedAnnotation#missing()} if none is present.
	 *
	 * @param annotationType the annotation type to get
	 * @return a {@link MergedAnnotation} instance
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType);

	/**
	 * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
	 * annotation or meta-annotation of the specified type, or
	 * {@link MergedAnnotation#missing()} if none is present.
	 *
	 * @param annotationType the annotation type to get
	 * @param predicate      a predicate that must match, or {@code null} if only
	 *                       type matching is required
	 * @return a {@link MergedAnnotation} instance
	 * @see MergedAnnotationPredicates
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
												   @Nullable Predicate<? super MergedAnnotation<A>> predicate);

	/**
	 * Get a matching annotation or meta-annotation of the specified type, or
	 * {@link MergedAnnotation#missing()} if none is present.
	 *
	 * @param annotationType the annotation type to get
	 * @param predicate      a predicate that must match, or {@code null} if only
	 *                       type matching is required
	 * @param selector       a selector used to choose the most appropriate annotation
	 *                       within an aggregate, or {@code null} to select the
	 *                       {@linkplain MergedAnnotationSelectors#nearest() nearest}
	 * @return a {@link MergedAnnotation} instance
	 * @see MergedAnnotationPredicates
	 * @see MergedAnnotationSelectors
	 */
	<A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
												   @Nullable Predicate<? super MergedAnnotation<A>> predicate,
												   @Nullable MergedAnnotationSelector<A> selector);

	/**
	 * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
	 * annotation or meta-annotation of the specified type, or
	 * {@link MergedAnnotation#missing()} if none is present.
	 *
	 * @param annotationType the fully qualified class name of the annotation type
	 *                       to get
	 * @return a {@link MergedAnnotation} instance
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType);

	/**
	 * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
	 * annotation or meta-annotation of the specified type, or
	 * {@link MergedAnnotation#missing()} if none is present.
	 *
	 * @param annotationType the fully qualified class name of the annotation type
	 *                       to get
	 * @param predicate      a predicate that must match, or {@code null} if only
	 *                       type matching is required
	 * @return a {@link MergedAnnotation} instance
	 * @see MergedAnnotationPredicates
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType,
												   @Nullable Predicate<? super MergedAnnotation<A>> predicate);

	/**
	 * Get a matching annotation or meta-annotation of the specified type, or
	 * {@link MergedAnnotation#missing()} if none is present.
	 *
	 * @param annotationType the fully qualified class name of the annotation type
	 *                       to get
	 * @param predicate      a predicate that must match, or {@code null} if only
	 *                       type matching is required
	 * @param selector       a selector used to choose the most appropriate annotation
	 *                       within an aggregate, or {@code null} to select the
	 *                       {@linkplain MergedAnnotationSelectors#nearest() nearest}
	 * @return a {@link MergedAnnotation} instance
	 * @see MergedAnnotationPredicates
	 * @see MergedAnnotationSelectors
	 */
	<A extends Annotation> MergedAnnotation<A> get(String annotationType,
												   @Nullable Predicate<? super MergedAnnotation<A>> predicate,
												   @Nullable MergedAnnotationSelector<A> selector);

	/**
	 * Stream all annotations and meta-annotations that match the specified
	 * type. The resulting stream follows the same ordering rules as
	 * {@link #stream()}.
	 *
	 * @param annotationType the annotation type to match
	 * @return a stream of matching annotations
	 */
	<A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> annotationType);

	/**
	 * Stream all annotations and meta-annotations that match the specified
	 * type. The resulting stream follows the same ordering rules as
	 * {@link #stream()}.
	 *
	 * @param annotationType the fully qualified class name of the annotation type
	 *                       to match
	 * @return a stream of matching annotations
	 */
	<A extends Annotation> Stream<MergedAnnotation<A>> stream(String annotationType);

	/**
	 * Stream all annotations and meta-annotations contained in this collection.
	 * The resulting stream is ordered first by the
	 * {@linkplain MergedAnnotation#getAggregateIndex() aggregate index} and then
	 * by the annotation distance (with the closest annotations first). This ordering
	 * means that, for most use-cases, the most suitable annotations appear
	 * earliest in the stream.
	 *
	 * @return a stream of annotations
	 */
	Stream<MergedAnnotation<Annotation>> stream();


	/**
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified element. The
	 * resulting instance will not include any inherited annotations. If you
	 * want to include those as well you should use
	 * {@link #from(AnnotatedElement, SearchStrategy)} with an appropriate
	 * {@link SearchStrategy}.
	 *
	 * @param element the source element
	 * @return a {@link MergedAnnotations} instance containing the element's
	 * annotations
	 */
	static MergedAnnotations from(AnnotatedElement element) {
		return from(element, SearchStrategy.DIRECT);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified element and,
	 * depending on the {@link SearchStrategy}, related inherited elements.
	 *
	 * @param element        the source element
	 * @param searchStrategy the search strategy to use
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * element annotations
	 */
	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy) {
		return from(element, searchStrategy, RepeatableContainers.standardRepeatables());
	}

	/**
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified element and,
	 * depending on the {@link SearchStrategy}, related inherited elements.
	 *
	 * @param element              the source element
	 * @param searchStrategy       the search strategy to use
	 * @param repeatableContainers the repeatable containers that may be used by
	 *                             the element annotations or the meta-annotations
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * element annotations
	 */
	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy,
								  RepeatableContainers repeatableContainers) {

		return from(element, searchStrategy, repeatableContainers, AnnotationFilter.PLAIN);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance containing all
	 * annotations and meta-annotations from the specified element and,
	 * depending on the {@link SearchStrategy}, related inherited elements.
	 *
	 * @param element              the source element
	 * @param searchStrategy       the search strategy to use
	 * @param repeatableContainers the repeatable containers that may be used by
	 *                             the element annotations or the meta-annotations
	 * @param annotationFilter     an annotation filter used to restrict the
	 *                             annotations considered
	 * @return a {@link MergedAnnotations} instance containing the merged
	 * annotations for the supplied element
	 */
	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy,
								  RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

		Assert.notNull(repeatableContainers, "RepeatableContainers must not be null");
		Assert.notNull(annotationFilter, "AnnotationFilter must not be null");
		return TypeMappedAnnotations.from(element, searchStrategy, repeatableContainers, annotationFilter);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotations.
	 *
	 * @param annotations the annotations to include
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 * @see #from(Object, Annotation...)
	 */
	static MergedAnnotations from(Annotation... annotations) {
		return from(annotations, annotations);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotations.
	 *
	 * @param source      the source for the annotations. This source is used only
	 *                    for information and logging. It does not need to <em>actually</em>
	 *                    contain the specified annotations, and it will not be searched.
	 * @param annotations the annotations to include
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 * @see #from(Annotation...)
	 * @see #from(AnnotatedElement)
	 */
	static MergedAnnotations from(Object source, Annotation... annotations) {
		return from(source, annotations, RepeatableContainers.standardRepeatables());
	}

	/**
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotations.
	 *
	 * @param source               the source for the annotations. This source is used only
	 *                             for information and logging. It does not need to <em>actually</em>
	 *                             contain the specified annotations, and it will not be searched.
	 * @param annotations          the annotations to include
	 * @param repeatableContainers the repeatable containers that may be used by
	 *                             meta-annotations
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 */
	static MergedAnnotations from(Object source, Annotation[] annotations, RepeatableContainers repeatableContainers) {
		return from(source, annotations, repeatableContainers, AnnotationFilter.PLAIN);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * annotations.
	 *
	 * @param source               the source for the annotations. This source is used only
	 *                             for information and logging. It does not need to <em>actually</em>
	 *                             contain the specified annotations, and it will not be searched.
	 * @param annotations          the annotations to include
	 * @param repeatableContainers the repeatable containers that may be used by
	 *                             meta-annotations
	 * @param annotationFilter     an annotation filter used to restrict the
	 *                             annotations considered
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 */
	static MergedAnnotations from(Object source, Annotation[] annotations,
								  RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

		Assert.notNull(repeatableContainers, "RepeatableContainers must not be null");
		Assert.notNull(annotationFilter, "AnnotationFilter must not be null");
		return TypeMappedAnnotations.from(source, annotations, repeatableContainers, annotationFilter);
	}

	/**
	 * Create a new {@link MergedAnnotations} instance from the specified
	 * collection of directly present annotations. This method allows a
	 * {@link MergedAnnotations} instance to be created from annotations that
	 * are not necessarily loaded using reflection. The provided annotations
	 * must all be {@link MergedAnnotation#isDirectlyPresent() directly present}
	 * and must have an {@link MergedAnnotation#getAggregateIndex() aggregate
	 * index} of {@code 0}.
	 * <p>The resulting {@link MergedAnnotations} instance will contain both the
	 * specified annotations, and any meta-annotations that can be read using
	 * reflection.
	 *
	 * @param annotations the annotations to include
	 * @return a {@link MergedAnnotations} instance containing the annotations
	 * @see MergedAnnotation#of(ClassLoader, Object, Class, java.util.Map)
	 */
	static MergedAnnotations of(Collection<MergedAnnotation<?>> annotations) {
		return MergedAnnotationsCollection.of(annotations);
	}


	/**
	 * 查找策略，支持由 {@link MergedAnnotations#from(AnnotatedElement, SearchStrategy)}.
	 *
	 * <p> 每种策略创建一组不同的聚合，这些聚合将组合成最终的 {@link MergedAnnotations}.
	 */
	enum SearchStrategy {

		/**
		 * 只查找直接声明的注解，不考虑 {@link Inherited @Inherited} 注解，
		 * 也不搜索超类或实现的接口。
		 */
		DIRECT,

		/**
		 * 查找所有直接声明的注解以及任何 {@link Inherited @Inherited} 超类注解。
		 * 此策略仅在与 {@link Class} 类型一起使用时真正有用，因为对于所有其他 {@linkplain AnnotatedElement 注解元素}，
		 * {@link Inherited @Inherited} 注解被忽略。此策略不搜索实现的接口。
		 */
		INHERITED_ANNOTATIONS,

		/**
		 * 查找所有直接声明和超类注解。此策略类似于 {@link #INHERITED_ANNOTATIONS}，
		 * 但注解不需要使用 {@link Inherited @Inherited} 元注解。此策略不搜索实现的接口。
		 */
		SUPERCLASS,

		/**
		 * 在整个类型层次结构中进行完整搜索，包括超类和实现的接口。
		 * 超类注解不需要使用 {@link Inherited @Inherited} 元注解。
		 */
		TYPE_HIERARCHY,

		/**
		 * 定义了注解搜索策略。
		 * <p>
		 * 此枚举值表示搜索应包含源的整个类型层次结构，以及源的任何封闭类。与 {@link #TYPE_HIERARCHY} 相比，它增加了对封闭类上注解的搜索。对于超类上的注解，不要求它们必须使用 {@link Inherited @Inherited} 元注解。当在 {@link Method} 源上搜索注解时，此策略的行为与 {@link #TYPE_HIERARCHY} 相同。
		 */
		TYPE_HIERARCHY_AND_ENCLOSING_CLASSES
	}

}
