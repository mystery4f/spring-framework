/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 用于类型转换的上下文描述器。
 * <p>能够表示数组和泛型集合类型。
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @see ConversionService#canConvert(TypeDescriptor, TypeDescriptor)
 * @see ConversionService#convert(Object, TypeDescriptor, TypeDescriptor)
 * @since 3.0
 */
public class TypeDescriptor implements Serializable {

	private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

	private static final Map<Class<?>, TypeDescriptor> commonTypesCache = new HashMap<>(32);

	private static final Class<?>[] CACHED_COMMON_TYPES = {
			boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
			double.class, Double.class, float.class, Float.class, int.class, Integer.class,
			long.class, Long.class, short.class, Short.class, String.class, Object.class};

	static {
		for (Class<?> preCachedClass : CACHED_COMMON_TYPES) {
			commonTypesCache.put(preCachedClass, valueOf(preCachedClass));
		}
	}


	private final Class<?> type;

	private final ResolvableType resolvableType;

	private final AnnotatedElementAdapter annotatedElement;


	/**
	 * 根据 {@link MethodParameter} 创建一个新的类型描述符。
	 * <p>当源或目标转换点是构造函数参数、方法参数或方法返回值时，使用这个构造函数。
	 * @param methodParameter 方法参数
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		this.resolvableType = ResolvableType.forMethodParameter(methodParameter);
		this.type = this.resolvableType.resolve(methodParameter.getNestedParameterType());
		this.annotatedElement = new AnnotatedElementAdapter(methodParameter.getParameterIndex() == -1 ?
				methodParameter.getMethodAnnotations() : methodParameter.getParameterAnnotations());
	}

	/**
	 * 根据一个{@link Field}字段创建一个新的类型描述符。
	 * <p>当源或目标转换点是一个字段时，使用这个构造函数。
	 *
	 * @param field 字段，用于创建类型描述符。
	 */
	public TypeDescriptor(Field field) {
		this.resolvableType = ResolvableType.forField(field);
		this.type = this.resolvableType.resolve(field.getType());
		this.annotatedElement = new AnnotatedElementAdapter(field.getAnnotations());
	}

	/**
	 * 根据一个{@link Property}属性创建一个新的类型描述符。
	 * <p>当源或目标转换点是Java类上的属性时，使用这个构造函数。
	 *
	 * @param property 属性对象，表示将要创建类型描述符的属性。
	 */
	public TypeDescriptor(Property property) {
		Assert.notNull(property, "Property must not be null");
		this.resolvableType = ResolvableType.forMethodParameter(property.getMethodParameter());
		this.type = this.resolvableType.resolve(property.getType());
		this.annotatedElement = new AnnotatedElementAdapter(property.getAnnotations());
	}

	/**
	 * 根据一个{@link ResolvableType}创建一个新的类型描述符。
	 * <p>此构造函数内部使用，并且也可以被支持非Java语言且具有扩展类型系统的子类使用。
	 * 从5.1.4版本开始，它被设置为公共的，而在之前它是受保护的。
	 *
	 * @param resolvableType 可解析的类型，提供动态类型解析能力。
	 * @param type            支持的后端类型（如果为{@code null}，则表示需要解析）。
	 * @param annotations     类型注解。
	 * @since 4.0
	 */
	public TypeDescriptor(ResolvableType resolvableType, @Nullable Class<?> type, @Nullable Annotation[] annotations) {
		this.resolvableType = resolvableType;
		this.type = (type != null ? type : resolvableType.toClass());
		this.annotatedElement = new AnnotatedElementAdapter(annotations);
	}

	/**
	 * 为一个对象创建一个新的类型描述符。
	 * <p>使用这个工厂方法可以在要求转换系统将源对象转换为另一种类型之前对源对象进行反射检查。
	 * <p>如果提供的对象为{@code null}，则返回{@code null}，否则调用
	 * {@link #valueOf(Class)}从对象的类构建一个类型描述符。
	 *
	 * @param source 源对象
	 * @return 类型描述符，如果源对象为null，则返回null
	 */
	@Nullable
	public static TypeDescriptor forObject(@Nullable Object source) {
	    // 根据源对象是否为null，决定是返回null还是调用valueOf方法获取类型描述符
	    return (source != null ? valueOf(source.getClass()) : null);
	}

	/**
	 * 根据给定的类型创建一个新的类型描述符。
	 * <p>当没有类型位置（如方法参数或字段）来提供额外的转换上下文时，使用此方法来指示转换系统将对象转换为特定的目标类型。
	 * <p>通常，更推荐使用 {@link #forObject(Object)} 来从源对象构造类型描述符，因为它处理了 {@code null} 对象的情况。
	 *
	 * @param type 类型（如果为 {@code null}，则表示 {@code Object.class}）
	 * @return 对应的类型描述符
	 */
	public static TypeDescriptor valueOf(@Nullable Class<?> type) {
	    // 如果类型为null，则将其视为Object类
	    if (type == null) {
	        type = Object.class;
	    }
	    // 从缓存中尝试获取类型描述符
	    TypeDescriptor desc = commonTypesCache.get(type);
	    // 如果缓存中存在，则使用缓存中的描述符；否则，创建一个新的类型描述符
	    return (desc != null ? desc : new TypeDescriptor(ResolvableType.forClass(type), null, null));
	}

	/**
	 * 根据给定的{@link java.util.Collection}类型创建一个新的类型描述符。
	 * <p>这对于将类型化的Collections非常有用。
	 * <p>例如，一个{@code List<String>}可以转换为一个{@code List<EmailAddress>}，
	 * 通过使用此方法构建目标类型来实现。构建此类{@code TypeDescriptor}的代码调用可能如下所示：
	 * {@code collection(List.class, TypeDescriptor.valueOf(EmailAddress.class));}
	 *
	 * @param collectionType        集合类型，必须实现{@link Collection}接口。
	 * @param elementTypeDescriptor 集合元素类型的描述符，用于转换集合元素的类型。
	 * @return 集合类型描述符。
	 * @throws IllegalArgumentException 如果collectionType不是java.util.Collection的子类。
	 * @throws NullPointerException     如果collectionType为null。
	 */
	public static TypeDescriptor collection(Class<?> collectionType, @Nullable TypeDescriptor elementTypeDescriptor) {
		// 确保集合类型不为null
	    Assert.notNull(collectionType, "Collection type must not be null");
		// 确保类型是Collection的子类
	    if (!Collection.class.isAssignableFrom(collectionType)) {
	        throw new IllegalArgumentException("Collection type must be a [java.util.Collection]");
	    }
		// 获取元素的可解析类型
	    ResolvableType element = (elementTypeDescriptor != null ? elementTypeDescriptor.resolvableType : null);
		// 创建并返回集合类型的描述符
	    return new TypeDescriptor(ResolvableType.forClassWithGenerics(collectionType, element), null, null);
	}

	/**
	 * 根据给定的 {@link java.util.Map} 类型创建一个新的类型描述符。
	 * <p>这对于转换为有类型的Map非常有用。
	 * <p>例如，一个 Map&lt;String, String&gt; 可以通过使用此方法构建的 targetType 转换为 Map&lt;Id, EmailAddress&gt;:
	 * 调用此方法构建此类类型描述符的代码可能看起来像这样：
	 * <pre class="code">
	 * map(Map.class, TypeDescriptor.valueOf(Id.class), TypeDescriptor.valueOf(EmailAddress.class));
	 * </pre>
	 *
	 * @param mapType             必须实现 {@link Map} 的映射类型。
	 * @param keyTypeDescriptor   映射键类型的描述符，用于转换映射键。
	 * @param valueTypeDescriptor 映射的值类型，用于转换映射值。
	 * @return 映射类型描述符。
	 * @throws IllegalArgumentException 如果 mapType 不是 [java.util.Map] 的子类。
	 */
	public static TypeDescriptor map(Class<?> mapType, @Nullable TypeDescriptor keyTypeDescriptor,
	                                  @Nullable TypeDescriptor valueTypeDescriptor) {
	    // 确保映射类型不为null，并且确实是Map的子类
	    Assert.notNull(mapType, "Map type must not be null");
	    if (!Map.class.isAssignableFrom(mapType)) {
	        throw new IllegalArgumentException("Map type must be a [java.util.Map]");
	    }
	    // 获取键和值的ResolvableType，如果描述符存在的话
	    ResolvableType key = (keyTypeDescriptor != null ? keyTypeDescriptor.resolvableType : null);
	    ResolvableType value = (valueTypeDescriptor != null ? valueTypeDescriptor.resolvableType : null);
	    // 创建并返回一个新的类型描述符，带有映射的泛型参数
	    return new TypeDescriptor(ResolvableType.forClassWithGenerics(mapType, key, value), null, null);
	}

	/**
	 * 创建一个新的类型描述符，作为指定类型的数组。
	 * <p>例如，要创建一个 {@code Map<String,String>[]}，可以使用以下代码：
	 * <pre class="code">
	 * TypeDescriptor.array(TypeDescriptor.map(Map.class, TypeDescriptor.value(String.class), TypeDescriptor.value(String.class)));
	 * </pre>
	 *
	 * @param elementTypeDescriptor 数组元素的 {@link TypeDescriptor}，或者为 {@code null}
	 * @return 一个数组 {@link TypeDescriptor}，如果 {@code elementTypeDescriptor} 为 {@code null} 则返回 {@code null}
	 * @since 3.2.1
	 */
	@Nullable
	public static TypeDescriptor array(@Nullable TypeDescriptor elementTypeDescriptor) {
	    // 当元素类型描述符为null时，直接返回null，否则创建一个新的数组类型描述符
	    if (elementTypeDescriptor == null) {
	        return null;
	    }
	    return new TypeDescriptor(ResolvableType.forArrayComponent(elementTypeDescriptor.resolvableType),
	            null, elementTypeDescriptor.getAnnotations()
	    );
	}

	/**
	 * 为方法参数中声明的嵌套类型创建类型描述符。
	 * <p>例如，如果方法参数是{@code List<String>}，且嵌套级别为1，
	 * 则嵌套类型描述符将是String.class。
	 * <p>如果方法参数是{@code List<List<String>>}，且嵌套级别为2，
	 * 则嵌套类型描述符也将是String.class。
	 * <p>如果方法参数是{@code Map<Integer, String>}，且嵌套级别为1，
	 * 则嵌套类型描述符将是从map值派生出的String类型。
	 * <p>如果方法参数是{@code List<Map<Integer, String>>}，且嵌套级别为2，
	 * 则嵌套类型描述符也将是從map值派生出的String类型。
	 * <p>如果无法获取嵌套类型（例如，方法参数是{@code List<?>}），则返回{@code null}。
	 *
	 * @param methodParameter 方法参数，其嵌套级别为1
	 * @param nestingLevel 嵌套类型的级别，在方法参数内的集合/数组元素或
	 *                     映射键/值声明的嵌套级别
	 * @return 指定嵌套级别的类型描述符，如果无法获取则返回{@code null}
	 * @throws IllegalArgumentException 如果输入的{@link MethodParameter}参数的嵌套级别不是1，
	 *                                  或者指定嵌套级别之前的类型不是集合、数组或映射类型
	 */
	@Nullable
	public static TypeDescriptor nested(MethodParameter methodParameter, int nestingLevel) {
	    // 校验方法参数的嵌套级别是否为1，不满足则抛出异常
	    if (methodParameter.getNestingLevel() != 1) {
	        throw new IllegalArgumentException("MethodParameter nesting level must be 1: " +
	                "use the nestingLevel parameter to specify the desired nestingLevel for nested type traversal");
	    }
	    // 使用给定的方法参数和嵌套级别获取嵌套类型的描述符
	    return nested(new TypeDescriptor(methodParameter), nestingLevel);
	}

	@Nullable
	private static TypeDescriptor nested(TypeDescriptor typeDescriptor, int nestingLevel) {
		ResolvableType nested = typeDescriptor.resolvableType;
		for (int i = 0; i < nestingLevel; i++) {
			if (Object.class == nested.getType()) {
				// Could be a collection type but we don't know about its element type,
				// so let's just assume there is an element type of type Object...
			} else {
				nested = nested.getNested(2);
			}
		}
		if (nested == ResolvableType.NONE) {
			return null;
		}
		return getRelatedIfResolvable(typeDescriptor, nested);
	}

	/**
	 * Create a type descriptor for a nested type declared within the field.
	 * <p>For example, if the field is a {@code List<String>} and the nesting
	 * level is 1, the nested type descriptor will be {@code String.class}.
	 * <p>If the field is a {@code List<List<String>>} and the nesting level is
	 * 2, the nested type descriptor will also be a {@code String.class}.
	 * <p>If the field is a {@code Map<Integer, String>} and the nesting level
	 * is 1, the nested type descriptor will be String, derived from the map value.
	 * <p>If the field is a {@code List<Map<Integer, String>>} and the nesting
	 * level is 2, the nested type descriptor will be String, derived from the map value.
	 * <p>Returns {@code null} if a nested type cannot be obtained because it was not
	 * declared. For example, if the field is a {@code List<?>}, the nested type
	 * descriptor returned will be {@code null}.
	 *
	 * @param field        the field
	 * @param nestingLevel the nesting level of the collection/array element or
	 *                     map key/value declaration within the field
	 * @return the nested type descriptor at the specified nesting level,
	 * or {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the types up to the specified nesting
	 *                                  level are not of collection, array, or map types
	 */
	@Nullable
	public static TypeDescriptor nested(Field field, int nestingLevel) {
		return nested(new TypeDescriptor(field), nestingLevel);
	}

	/**
	 * Create a type descriptor for a nested type declared within the property.
	 * <p>For example, if the property is a {@code List<String>} and the nesting
	 * level is 1, the nested type descriptor will be {@code String.class}.
	 * <p>If the property is a {@code List<List<String>>} and the nesting level
	 * is 2, the nested type descriptor will also be a {@code String.class}.
	 * <p>If the property is a {@code Map<Integer, String>} and the nesting level
	 * is 1, the nested type descriptor will be String, derived from the map value.
	 * <p>If the property is a {@code List<Map<Integer, String>>} and the nesting
	 * level is 2, the nested type descriptor will be String, derived from the map value.
	 * <p>Returns {@code null} if a nested type cannot be obtained because it was not
	 * declared. For example, if the property is a {@code List<?>}, the nested type
	 * descriptor returned will be {@code null}.
	 *
	 * @param property     the property
	 * @param nestingLevel the nesting level of the collection/array element or
	 *                     map key/value declaration within the property
	 * @return the nested type descriptor at the specified nesting level, or
	 * {@code null} if it could not be obtained
	 * @throws IllegalArgumentException if the types up to the specified nesting
	 *                                  level are not of collection, array, or map types
	 */
	@Nullable
	public static TypeDescriptor nested(Property property, int nestingLevel) {
		return nested(new TypeDescriptor(property), nestingLevel);
	}

	/**
	 * Variation of {@link #getType()} that accounts for a primitive type by
	 * returning its object wrapper type.
	 * <p>This is useful for conversion service implementations that wish to
	 * normalize to object-based types and not work with primitive types directly.
	 */
	public Class<?> getObjectType() {
		return ClassUtils.resolvePrimitiveIfNecessary(getType());
	}

	/**
	 * Return the underlying source of the descriptor. Will return a {@link Field},
	 * {@link MethodParameter} or {@link Type} depending on how the {@link TypeDescriptor}
	 * was constructed. This method is primarily to provide access to additional
	 * type information or meta-data that alternative JVM languages may provide.
	 *
	 * @since 4.0
	 */
	public Object getSource() {
		return this.resolvableType.getSource();
	}

	/**
	 * Cast this {@link TypeDescriptor} to a superclass or implemented interface
	 * preserving annotations and nested type context.
	 *
	 * @param superType the super type to cast to (can be {@code null})
	 * @return a new TypeDescriptor for the up-cast type
	 * @throws IllegalArgumentException if this type is not assignable to the super-type
	 * @since 3.2
	 */
	@Nullable
	public TypeDescriptor upcast(@Nullable Class<?> superType) {
		if (superType == null) {
			return null;
		}
		Assert.isAssignable(superType, getType());
		return new TypeDescriptor(getResolvableType().as(superType), superType, getAnnotations());
	}

	/**
	 * The type of the backing class, method parameter, field, or property
	 * described by this TypeDescriptor.
	 * <p>Returns primitive types as-is. See {@link #getObjectType()} for a
	 * variation of this operation that resolves primitive types to their
	 * corresponding Object types if necessary.
	 *
	 * @see #getObjectType()
	 */
	public Class<?> getType() {
		return this.type;
	}

	/**
	 * Return the underlying {@link ResolvableType}.
	 *
	 * @since 4.0
	 */
	public ResolvableType getResolvableType() {
		return this.resolvableType;
	}

	/**
	 * Return the annotations associated with this type descriptor, if any.
	 *
	 * @return the annotations, or an empty array if none
	 */
	public Annotation[] getAnnotations() {
		return this.annotatedElement.getAnnotations();
	}

	/**
	 * Return the name of this type: the fully qualified class name.
	 */
	public String getName() {
		return ClassUtils.getQualifiedName(getType());
	}

	/**
	 * Is this type a primitive type?
	 */
	public boolean isPrimitive() {
		return getType().isPrimitive();
	}

	/**
	 * Determine if this type descriptor has the specified annotation.
	 * <p>As of Spring Framework 4.2, this method supports arbitrary levels
	 * of meta-annotations.
	 *
	 * @param annotationType the annotation type
	 * @return <tt>true</tt> if the annotation is present
	 */
	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		if (this.annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
			// to return a copy of the array, whereas we can do it more efficiently here.
			return false;
		}
		return AnnotatedElementUtils.isAnnotated(this.annotatedElement, annotationType);
	}

	/**
	 * Obtain the annotation of the specified {@code annotationType} that is on this type descriptor.
	 * <p>As of Spring Framework 4.2, this method supports arbitrary levels of meta-annotations.
	 *
	 * @param annotationType the annotation type
	 * @return the annotation, or {@code null} if no such annotation exists on this type descriptor
	 */
	@Nullable
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		if (this.annotatedElement.isEmpty()) {
			// Shortcut: AnnotatedElementUtils would have to expect AnnotatedElement.getAnnotations()
			// to return a copy of the array, whereas we can do it more efficiently here.
			return null;
		}
		return AnnotatedElementUtils.getMergedAnnotation(this.annotatedElement, annotationType);
	}

	/**
	 * Returns true if an object of this type descriptor can be assigned to the location
	 * described by the given type descriptor.
	 * <p>For example, {@code valueOf(String.class).isAssignableTo(valueOf(CharSequence.class))}
	 * returns {@code true} because a String value can be assigned to a CharSequence variable.
	 * On the other hand, {@code valueOf(Number.class).isAssignableTo(valueOf(Integer.class))}
	 * returns {@code false} because, while all Integers are Numbers, not all Numbers are Integers.
	 * <p>For arrays, collections, and maps, element and key/value types are checked if declared.
	 * For example, a List&lt;String&gt; field value is assignable to a Collection&lt;CharSequence&gt;
	 * field, but List&lt;Number&gt; is not assignable to List&lt;Integer&gt;.
	 *
	 * @return {@code true} if this type is assignable to the type represented by the provided
	 * type descriptor
	 * @see #getObjectType()
	 */
	public boolean isAssignableTo(TypeDescriptor typeDescriptor) {
		boolean typesAssignable = typeDescriptor.getObjectType().isAssignableFrom(getObjectType());
		if (!typesAssignable) {
			return false;
		}
		if (isArray() && typeDescriptor.isArray()) {
			return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
		} else if (isCollection() && typeDescriptor.isCollection()) {
			return isNestedAssignable(getElementTypeDescriptor(), typeDescriptor.getElementTypeDescriptor());
		} else if (isMap() && typeDescriptor.isMap()) {
			return isNestedAssignable(getMapKeyTypeDescriptor(), typeDescriptor.getMapKeyTypeDescriptor()) &&
					isNestedAssignable(getMapValueTypeDescriptor(), typeDescriptor.getMapValueTypeDescriptor());
		} else {
			return true;
		}
	}

	private boolean isNestedAssignable(@Nullable TypeDescriptor nestedTypeDescriptor,
									   @Nullable TypeDescriptor otherNestedTypeDescriptor) {

		return (nestedTypeDescriptor == null || otherNestedTypeDescriptor == null ||
				nestedTypeDescriptor.isAssignableTo(otherNestedTypeDescriptor));
	}

	/**
	 * Is this type a {@link Collection} type?
	 */
	public boolean isCollection() {
		return Collection.class.isAssignableFrom(getType());
	}

	/**
	 * Is this type an array type?
	 */
	public boolean isArray() {
		return getType().isArray();
	}

	/**
	 * If this type is a {@link Collection} or an array, creates a element TypeDescriptor
	 * from the provided collection or array element.
	 * <p>Narrows the {@link #getElementTypeDescriptor() elementType} property to the class
	 * of the provided collection or array element. For example, if this describes a
	 * {@code java.util.List<java.lang.Number>} and the element argument is a
	 * {@code java.lang.Integer}, the returned TypeDescriptor will be {@code java.lang.Integer}.
	 * If this describes a {@code java.util.List<?>} and the element argument is a
	 * {@code java.lang.Integer}, the returned TypeDescriptor will be {@code java.lang.Integer}
	 * as well.
	 * <p>Annotation and nested type context will be preserved in the narrowed
	 * TypeDescriptor that is returned.
	 *
	 * @param element the collection or array element
	 * @return a element type descriptor, narrowed to the type of the provided element
	 * @see #getElementTypeDescriptor()
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor elementTypeDescriptor(Object element) {
		return narrow(element, getElementTypeDescriptor());
	}

	/**
	 * If this type is an array, returns the array's component type.
	 * If this type is a {@code Stream}, returns the stream's component type.
	 * If this type is a {@link Collection} and it is parameterized, returns the Collection's element type.
	 * If the Collection is not parameterized, returns {@code null} indicating the element type is not declared.
	 *
	 * @return the array component type or Collection element type, or {@code null} if this type is not
	 * an array type or a {@code java.util.Collection} or if its element type is not parameterized
	 * @see #elementTypeDescriptor(Object)
	 */
	@Nullable
	public TypeDescriptor getElementTypeDescriptor() {
		if (getResolvableType().isArray()) {
			return new TypeDescriptor(getResolvableType().getComponentType(), null, getAnnotations());
		}
		if (Stream.class.isAssignableFrom(getType())) {
			return getRelatedIfResolvable(this, getResolvableType().as(Stream.class).getGeneric(0));
		}
		return getRelatedIfResolvable(this, getResolvableType().asCollection().getGeneric(0));
	}

	@Nullable
	private static TypeDescriptor getRelatedIfResolvable(TypeDescriptor source, ResolvableType type) {
		if (type.resolve() == null) {
			return null;
		}
		return new TypeDescriptor(type, null, source.getAnnotations());
	}

	@Nullable
	private TypeDescriptor narrow(@Nullable Object value, @Nullable TypeDescriptor typeDescriptor) {
		if (typeDescriptor != null) {
			return typeDescriptor.narrow(value);
		}
		if (value != null) {
			return narrow(value);
		}
		return null;
	}

	/**
	 * Narrows this {@link TypeDescriptor} by setting its type to the class of the
	 * provided value.
	 * <p>If the value is {@code null}, no narrowing is performed and this TypeDescriptor
	 * is returned unchanged.
	 * <p>Designed to be called by binding frameworks when they read property, field,
	 * or method return values. Allows such frameworks to narrow a TypeDescriptor built
	 * from a declared property, field, or method return value type. For example, a field
	 * declared as {@code java.lang.Object} would be narrowed to {@code java.util.HashMap}
	 * if it was set to a {@code java.util.HashMap} value. The narrowed TypeDescriptor
	 * can then be used to convert the HashMap to some other type. Annotation and nested
	 * type context is preserved by the narrowed copy.
	 *
	 * @param value the value to use for narrowing this type descriptor
	 * @return this TypeDescriptor narrowed (returns a copy with its type updated to the
	 * class of the provided value)
	 */
	public TypeDescriptor narrow(@Nullable Object value) {
		if (value == null) {
			return this;
		}
		ResolvableType narrowed = ResolvableType.forType(value.getClass(), getResolvableType());
		return new TypeDescriptor(narrowed, value.getClass(), getAnnotations());
	}

	/**
	 * If this type is a {@link Map}, creates a mapKey {@link TypeDescriptor}
	 * from the provided map key.
	 * <p>Narrows the {@link #getMapKeyTypeDescriptor() mapKeyType} property
	 * to the class of the provided map key. For example, if this describes a
	 * {@code java.util.Map<java.lang.Number, java.lang.String>} and the key
	 * argument is a {@code java.lang.Integer}, the returned TypeDescriptor will be
	 * {@code java.lang.Integer}. If this describes a {@code java.util.Map<?, ?>}
	 * and the key argument is a {@code java.lang.Integer}, the returned
	 * TypeDescriptor will be {@code java.lang.Integer} as well.
	 * <p>Annotation and nested type context will be preserved in the narrowed
	 * TypeDescriptor that is returned.
	 *
	 * @param mapKey the map key
	 * @return the map key type descriptor
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor getMapKeyTypeDescriptor(Object mapKey) {
		return narrow(mapKey, getMapKeyTypeDescriptor());
	}

	/**
	 * If this type is a {@link Map} and its key type is parameterized,
	 * returns the map's key type. If the Map's key type is not parameterized,
	 * returns {@code null} indicating the key type is not declared.
	 *
	 * @return the Map key type, or {@code null} if this type is a Map
	 * but its key type is not parameterized
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 */
	@Nullable
	public TypeDescriptor getMapKeyTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(0));
	}

	/**
	 * Is this type a {@link Map} type?
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/**
	 * If this type is a {@link Map}, creates a mapValue {@link TypeDescriptor}
	 * from the provided map value.
	 * <p>Narrows the {@link #getMapValueTypeDescriptor() mapValueType} property
	 * to the class of the provided map value. For example, if this describes a
	 * {@code java.util.Map<java.lang.String, java.lang.Number>} and the value
	 * argument is a {@code java.lang.Integer}, the returned TypeDescriptor will be
	 * {@code java.lang.Integer}. If this describes a {@code java.util.Map<?, ?>}
	 * and the value argument is a {@code java.lang.Integer}, the returned
	 * TypeDescriptor will be {@code java.lang.Integer} as well.
	 * <p>Annotation and nested type context will be preserved in the narrowed
	 * TypeDescriptor that is returned.
	 *
	 * @param mapValue the map value
	 * @return the map value type descriptor
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 * @see #narrow(Object)
	 */
	@Nullable
	public TypeDescriptor getMapValueTypeDescriptor(Object mapValue) {
		return narrow(mapValue, getMapValueTypeDescriptor());
	}

	/**
	 * If this type is a {@link Map} and its value type is parameterized,
	 * returns the map's value type.
	 * <p>If the Map's value type is not parameterized, returns {@code null}
	 * indicating the value type is not declared.
	 *
	 * @return the Map value type, or {@code null} if this type is a Map
	 * but its value type is not parameterized
	 * @throws IllegalStateException if this type is not a {@code java.util.Map}
	 */
	@Nullable
	public TypeDescriptor getMapValueTypeDescriptor() {
		Assert.state(isMap(), "Not a [java.util.Map]");
		return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(1));
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypeDescriptor)) {
			return false;
		}
		TypeDescriptor otherDesc = (TypeDescriptor) other;
		if (getType() != otherDesc.getType()) {
			return false;
		}
		if (!annotationsMatch(otherDesc)) {
			return false;
		}
		if (isCollection() || isArray()) {
			return ObjectUtils.nullSafeEquals(getElementTypeDescriptor(), otherDesc.getElementTypeDescriptor());
		} else if (isMap()) {
			return (ObjectUtils.nullSafeEquals(getMapKeyTypeDescriptor(), otherDesc.getMapKeyTypeDescriptor()) &&
					ObjectUtils.nullSafeEquals(getMapValueTypeDescriptor(), otherDesc.getMapValueTypeDescriptor()));
		} else {
			return true;
		}
	}

	private boolean annotationsMatch(TypeDescriptor otherDesc) {
		Annotation[] anns = getAnnotations();
		Annotation[] otherAnns = otherDesc.getAnnotations();
		if (anns == otherAnns) {
			return true;
		}
		if (anns.length != otherAnns.length) {
			return false;
		}
		if (anns.length > 0) {
			for (int i = 0; i < anns.length; i++) {
				if (!annotationEquals(anns[i], otherAnns[i])) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean annotationEquals(Annotation ann, Annotation otherAnn) {
		// Annotation.equals is reflective and pretty slow, so let's check identity and proxy type first.
		return (ann == otherAnn || (ann.getClass() == otherAnn.getClass() && ann.equals(otherAnn)));
	}

	@Override
	public int hashCode() {
		return getType().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Annotation ann : getAnnotations()) {
			builder.append('@').append(ann.annotationType().getName()).append(' ');
		}
		builder.append(getResolvableType());
		return builder.toString();
	}

	/**
	 * Adapter class for exposing a {@code TypeDescriptor}'s annotations as an
	 * {@link AnnotatedElement}, in particular to {@link AnnotatedElementUtils}.
	 *
	 * @see AnnotatedElementUtils#isAnnotated(AnnotatedElement, Class)
	 * @see AnnotatedElementUtils#getMergedAnnotation(AnnotatedElement, Class)
	 */
	private class AnnotatedElementAdapter implements AnnotatedElement, Serializable {

		@Nullable
		private final Annotation[] annotations;

		public AnnotatedElementAdapter(@Nullable Annotation[] annotations) {
			this.annotations = annotations;
		}

		@Override
		public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
			for (Annotation annotation : getAnnotations()) {
				if (annotation.annotationType() == annotationClass) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Annotation[] getAnnotations() {
			return (this.annotations != null ? this.annotations.clone() : EMPTY_ANNOTATION_ARRAY);
		}

		@Override
		@Nullable
		@SuppressWarnings("unchecked")
		public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
			for (Annotation annotation : getAnnotations()) {
				if (annotation.annotationType() == annotationClass) {
					return (T) annotation;
				}
			}
			return null;
		}

		@Override
		public Annotation[] getDeclaredAnnotations() {
			return getAnnotations();
		}

		public boolean isEmpty() {
			return ObjectUtils.isEmpty(this.annotations);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof AnnotatedElementAdapter &&
					Arrays.equals(this.annotations, ((AnnotatedElementAdapter) other).annotations)));
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.annotations);
		}

		@Override
		public String toString() {
			return TypeDescriptor.this.toString();
		}
	}

}
