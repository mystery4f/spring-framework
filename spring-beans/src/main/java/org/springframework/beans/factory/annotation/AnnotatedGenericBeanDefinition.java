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

package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Extension of the {@link org.springframework.beans.factory.support.GenericBeanDefinition}
 * class, adding support for annotation metadata exposed through the
 * {@link AnnotatedBeanDefinition} interface.
 *
 * <p>This GenericBeanDefinition variant is mainly useful for testing code that expects
 * to operate on an AnnotatedBeanDefinition, for example strategy implementations
 * in Spring's component scanning support (where the default definition class is
 * {@link org.springframework.context.annotation.ScannedGenericBeanDefinition},
 * which also implements the AnnotatedBeanDefinition interface).
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see AnnotatedBeanDefinition#getMetadata()
 * @see org.springframework.core.type.StandardAnnotationMetadata
 */
@SuppressWarnings("serial")
public class AnnotatedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {

	/**
	 * 存储注解元数据的成员变量
	 */
	private final AnnotationMetadata metadata;

	/**
	 * 存储工厂方法元数据的成员变量，可以为null
	 */
	@Nullable
	private MethodMetadata factoryMethodMetadata;


	/**
	 * 为给定的bean类创建一个新的AnnotatedGenericBeanDefinition。
	 * @param beanClass 加载的bean类
	 */
	public AnnotatedGenericBeanDefinition(Class<?> beanClass) {
		setBeanClass(beanClass);
		this.metadata = AnnotationMetadata.introspect(beanClass);
	}


	/**
	 * 为给定的注解元数据创建一个新的AnnotatedGenericBeanDefinition，允许使用基于ASM的处理和避免对bean类的早期加载。
	 * 注意，这个构造函数在功能上等同于{@link org.springframework.context.annotation.ScannedGenericBeanDefinition ScannedGenericBeanDefinition}，但是后者的语义表明bean是通过组件扫描特别发现的，而不是其他方式。
	 * @param metadata 问题中的bean类的注解元数据
	 * @since 3.1.1
	 */
	public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata) {
		// 断言注解元数据不为空
		Assert.notNull(metadata, "AnnotationMetadata must not be null");
		// 如果注解元数据是标准注解元数据类型
		if (metadata instanceof StandardAnnotationMetadata) {
			// 设置bean的类
			setBeanClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
		}
		else {
			// 否则设置bean的类名
			setBeanClassName(metadata.getClassName());
		}
		this.metadata = metadata;
	}


	/**
	 * 为给定的注解元数据和基于注解类的工厂方法创建一个新的AnnotatedGenericBeanDefinition。
	 * @param metadata 问题中的bean类的注解元数据
	 * @param factoryMethodMetadata 选定的工厂方法的元数据
	 * @since 4.1.1
	 */
	public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata, MethodMetadata factoryMethodMetadata) {
		this(metadata);
		Assert.notNull(factoryMethodMetadata, "MethodMetadata must not be null");
		setFactoryMethodName(factoryMethodMetadata.getMethodName());
		this.factoryMethodMetadata = factoryMethodMetadata;
	}

	@Override
	public final AnnotationMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	@Nullable
	public final MethodMetadata getFactoryMethodMetadata() {
		return this.factoryMethodMetadata;
	}


}
