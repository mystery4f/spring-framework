/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.aop.framework.autoproxy;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 自动代理感知组件的实用工具。
 * 主要供框架内部使用。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see AbstractAutoProxyCreator
 */
public abstract class AutoProxyUtils {

	/**
	 * Bean 定义属性，可能指示给定的 bean 是否应该使用其目标类进行代理（如果首先对其进行代理）。
	 * 值为 {@code Boolean.TRUE} 或 {@code Boolean.FALSE}。
	 * <p>如果代理工厂为特定 bean 构建了目标类代理，并且希望强制该 bean 始终可以转换为其目标类（即使通过自动代理应用了 AOP 通知），
	 * 则代理工厂可以设置此属性。
	 * @see #shouldProxyTargetClass
	 */
	public static final String PRESERVE_TARGET_CLASS_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(AutoProxyUtils.class, "preserveTargetClass");

	/**
	 * Bean 定义属性，指示自动代理 bean 的原始目标类，例如用于内省接口代理后面的目标类上的注解。
	 *
	 * @see #determineTargetClass
	 * @since 4.2.3
	 */
	public static final String ORIGINAL_TARGET_CLASS_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(AutoProxyUtils.class, "originalTargetClass");


	/**
	 * 确定给定的 bean 是否应该使用其目标类而不是其接口进行代理。
	 * 检查相应 bean 定义的 {@link #PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" 属性}。
	 *
	 * @param beanFactory 包含的 ConfigurableListableBeanFactory
	 * @param beanName    bean 的名称
	 * @return 给定的 bean 是否应该使用其目标类进行代理
	 */
	public static boolean shouldProxyTargetClass(
			ConfigurableListableBeanFactory beanFactory, @Nullable String beanName) {

		if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
			BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
			return Boolean.TRUE.equals(bd.getAttribute(PRESERVE_TARGET_CLASS_ATTRIBUTE));
		}
		return false;
	}

	/**
	 * 确定指定 bean 的原始目标类，如果可能的话，
	 * 否则退回到普通的 {@code getType} 查找。
	 *
	 * @param beanFactory 包含的 ConfigurableListableBeanFactory
	 * @param beanName    bean 的名称
	 * @return 存储在 bean 定义中的原始目标类（如果有）
	 * @see org.springframework.beans.factory.BeanFactory#getType(String)
	 * @since 4.2.3
	 */
	@Nullable
	public static Class<?> determineTargetClass(
			ConfigurableListableBeanFactory beanFactory, @Nullable String beanName) {

		if (beanName == null) {
			return null;
		}
		if (beanFactory.containsBeanDefinition(beanName)) {
			BeanDefinition bd = beanFactory.getMergedBeanDefinition(beanName);
			Class<?> targetClass = (Class<?>) bd.getAttribute(ORIGINAL_TARGET_CLASS_ATTRIBUTE);
			if (targetClass != null) {
				return targetClass;
			}
		}
		return beanFactory.getType(beanName);
	}

	/**
	 * Expose the given target class for the specified bean, if possible.
	 * @param beanFactory the containing ConfigurableListableBeanFactory
	 * @param beanName the name of the bean
	 * @param targetClass the corresponding target class
	 * @since 4.2.3
	 */
	static void exposeTargetClass(
			ConfigurableListableBeanFactory beanFactory, @Nullable String beanName, Class<?> targetClass) {

		if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
			beanFactory.getMergedBeanDefinition(beanName).setAttribute(ORIGINAL_TARGET_CLASS_ATTRIBUTE, targetClass);
		}
	}

	/**
	 * 根据 {@link AutowireCapableBeanFactory#ORIGINAL_INSTANCE_SUFFIX} 确定给定的 bean 名称是否表示“原始实例”，
	 * 并跳过对它的任何代理尝试。
	 * @param beanName bean 的名称
	 * @param beanClass 相应的 bean 类
	 * @since 5.1
	 * @see AutowireCapableBeanFactory#ORIGINAL_INSTANCE_SUFFIX
	 */
	static boolean isOriginalInstance(String beanName, Class<?> beanClass) {
		if (!StringUtils.hasLength(beanName) || beanName.length() !=
				beanClass.getName().length() + AutowireCapableBeanFactory.ORIGINAL_INSTANCE_SUFFIX.length()) {
			return false;
		}
		return (beanName.startsWith(beanClass.getName()) &&
				beanName.endsWith(AutowireCapableBeanFactory.ORIGINAL_INSTANCE_SUFFIX));
	}

}
