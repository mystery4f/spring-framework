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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods that are useful for bean definition reader implementations.
 * Mainly intended for internal use.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see PropertiesBeanDefinitionReader
 * @see org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader
 * @since 1.1
 */
public abstract class BeanDefinitionReaderUtils {

	/**
	 * 生成的bean名称的分隔符。如果类名称或父名称不是唯一的，
	 * 将会附加"#1"，"#2"等，直到名称变得唯一为止。
	 */
	public static final String GENERATED_BEAN_NAME_SEPARATOR = BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;

	/**
	 * 为给定的父名称和类名称创建一个新的GenericBeanDefinition，
	 * 如果指定了ClassLoader，则立即加载bean类。
	 *
	 * @param parentName  父bean的名称（如果有）
	 * @param className   bean类的名称（如果有）
	 * @param classLoader 用于加载bean类的ClassLoader
	 *                    （可以为null，只通过名称注册bean类）
	 * @return bean定义
	 * @throws ClassNotFoundException 如果无法加载bean类
	 */
	public static AbstractBeanDefinition createBeanDefinition(
			@Nullable String parentName, @Nullable String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setParentName(parentName);
		if (className != null) {
			if (classLoader != null) {
				bd.setBeanClass(ClassUtils.forName(className, classLoader));
			} else {
				bd.setBeanClassName(className);
			}
		}
		return bd;
	}

	/**
	 * 为给定的顶级bean定义生成一个唯一的bean名称，
	 * 在给定的bean工厂中是唯一的。
	 *
	 * @param beanDefinition 要为其生成bean名称的bean定义
	 * @param registry       将要注册该定义的bean工厂（用于检查现有的bean名称）
	 * @return 生成的bean名称
	 * @throws BeanDefinitionStoreException 如果无法为给定的bean定义生成唯一名称
	 * @see #generateBeanName(BeanDefinition, BeanDefinitionRegistry, boolean)
	 */
	public static String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {
		return generateBeanName(beanDefinition, registry, false);
	}

	/**
	 * 生成 Bean 名称的方法
	 *
	 * @param definition  Bean 的定义
	 * @param registry    Bean 定义注册器
	 * @param isInnerBean 是否为内部 Bean
	 * @return 生成的 Bean 名称
	 * @throws BeanDefinitionStoreException 如果没有定义 Bean 的类名或父类名或工厂Bean名，则抛出该异常
	 * @see AbstractBeanDefinition#getBeanClassName()
	 * @see AbstractBeanDefinition#getParentName()
	 * @see AbstractBeanDefinition#getFactoryBeanName()
	 */
	public static String generateBeanName(
			BeanDefinition definition, BeanDefinitionRegistry registry, boolean isInnerBean)
			throws BeanDefinitionStoreException {

		// 首先获取 Bean 的类名
		String generatedBeanName = definition.getBeanClassName();

		// 如果类名为 null，则根据 parentName 或 factoryBeanName 创建类名
		if (generatedBeanName == null) {
			if (definition.getParentName() != null) {
				generatedBeanName = definition.getParentName() + "$child";
			} else if (definition.getFactoryBeanName() != null) {
				generatedBeanName = definition.getFactoryBeanName() + "$created";
			}
		}

		// 如果生成的类名为空，则抛出异常
		if (!StringUtils.hasText(generatedBeanName)) {
			throw new BeanDefinitionStoreException("Unnamed bean definition specifies neither " +
					"'class' nor 'parent' nor 'factory-bean' - can't generate bean name");
		}

		// 如果是内部 Bean，则在类名后添加类的唯一标识符
		if (isInnerBean) {
			return generatedBeanName + GENERATED_BEAN_NAME_SEPARATOR +
					ObjectUtils.getIdentityHexString(definition);
		}

		// 如果是顶层 Bean，则在类名后添加唯一的后缀
		return uniqueBeanName(generatedBeanName, registry);
	}


	/**
	 * Turn the given bean name into a unique bean name for the given bean factory,
	 * appending a unique counter as suffix if necessary.
	 *
	 * @param beanName the original bean name
	 * @param registry the bean factory that the definition is going to be
	 *                 registered with (to check for existing bean names)
	 * @return the unique bean name to use
	 * @since 5.1
	 */
	public static String uniqueBeanName(String beanName, BeanDefinitionRegistry registry) {
		String id = beanName;
		int counter = -1;

		// Increase counter until the id is unique.
		String prefix = beanName + GENERATED_BEAN_NAME_SEPARATOR;
		while (counter == -1 || registry.containsBeanDefinition(id)) {
			counter++;
			id = prefix + counter;
		}
		return id;
	}

	/**
	 * 将给定的bean定义与给定的bean工厂进行注册。
	 *
	 * @param definitionHolder 包括名称和别名的bean定义
	 * @param registry         要注册的bean工厂
	 * @throws BeanDefinitionStoreException 如果注册失败
	 */
	public static void registerBeanDefinition(
			BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		// 在名称下注册bean定义。
		String beanName = definitionHolder.getBeanName();
		registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

		// 注册bean名称的别名（如果有）。
		String[] aliases = definitionHolder.getAliases();
		if (aliases != null) {
			for (String alias : aliases) {
				registry.registerAlias(beanName, alias);
			}
		}
	}

	/**
	 * 使用生成的名称将给定的bean定义注册到给定的bean工厂中，
	 * 生成的名称在该bean工厂中是唯一的。
	 *
	 * @param definition 要为其生成bean名称的bean定义
	 * @param registry   要注册的bean工厂
	 * @return 生成的bean名称
	 * @throws BeanDefinitionStoreException 如果无法为给定的bean定义生成唯一名称，
	 *                                      或者无法注册该定义
	 */
	public static String registerWithGeneratedName(
			AbstractBeanDefinition definition, BeanDefinitionRegistry registry)
			throws BeanDefinitionStoreException {

		String generatedName = generateBeanName(definition, registry, false);
		registry.registerBeanDefinition(generatedName, definition);
		return generatedName;
	}

}
