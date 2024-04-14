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

package org.springframework.context.annotation;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Interface to be implemented by types that register additional bean definitions when
 * processing @{@link Configuration} classes. Useful when operating at the bean definition
 * level (as opposed to {@code @Bean} method/instance level) is desired or necessary.
 *
 * <p>Along with {@code @Configuration} and {@link ImportSelector}, classes of this type
 * may be provided to the @{@link Import} annotation (or may also be returned from an
 * {@code ImportSelector}).
 *
 * <p>An {@link ImportBeanDefinitionRegistrar} may implement any of the following
 * {@link org.springframework.beans.factory.Aware Aware} interfaces, and their respective
 * methods will be called prior to {@link #registerBeanDefinitions}:
 * <ul>
 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}
 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}
 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}
 * </ul>
 *
 * <p>Alternatively, the class may provide a single constructor with one or more of
 * the following supported parameter types:
 * <ul>
 * <li>{@link org.springframework.core.env.Environment Environment}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactory BeanFactory}</li>
 * <li>{@link java.lang.ClassLoader ClassLoader}</li>
 * <li>{@link org.springframework.core.io.ResourceLoader ResourceLoader}</li>
 * </ul>
 *
 * <p>See implementations and associated unit tests for usage examples.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see Import
 * @see ImportSelector
 * @see Configuration
 * @since 3.1
 */
public interface ImportBeanDefinitionRegistrar {

	/**
	 * 根据导入的{@code @Configuration}类的注解元数据，根据需要注册bean定义。
	 * <p>注意，由于与{@code @Configuration}类处理相关的生命周期约束，此处可能不会注册{@link BeanDefinitionRegistryPostProcessor}类型。
	 * <p>默认实现委托给{@link #registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry)}。
	 *
	 * @param importingClassMetadata  导入类的注解元数据
	 * @param registry                当前的bean定义注册表
	 * @param importBeanNameGenerator 导入bean的bean名称生成策略：
	 *                                默认为{@link ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR}，或者如果已设置{@link ConfigurationClassPostProcessor#setBeanNameGenerator}，
	 *                                则为用户提供的策略。在后一种情况下，传入的策略将与包含应用程序上下文中的组件扫描使用的相同（否则，默认的组件扫描命名策略为{@link AnnotationBeanNameGenerator#INSTANCE}）。
	 * @see ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR
	 * @see ConfigurationClassPostProcessor#setBeanNameGenerator
	 * @since 5.2
	 */
	default void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
										 BeanNameGenerator importBeanNameGenerator) {

		// 调用另一个重载方法，将导入类的注解元数据和当前的bean定义注册表传递给它
		registerBeanDefinitions(importingClassMetadata, registry);
	}

	/**
	 * 根据导入的{@code @Configuration}类的注解元数据，根据需要注册bean定义。
	 * <p>注意，由于与{@code @Configuration}类处理相关的生命周期约束，此处可能不会注册{@link BeanDefinitionRegistryPostProcessor}类型。
	 * <p>默认实现为空。
	 *
	 * @param importingClassMetadata 导入类的注解元数据
	 * @param registry               当前的bean定义注册表
	 */
	default void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
	}

}
