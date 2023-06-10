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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * MergedBeanDefinitionPostProcessor是一个后处理器的回调接口，用于在运行时处理已合并的bean定义。
 * {@link BeanPostProcessor}的实现可以实现此子接口，以便处理Spring {@code BeanFactory}用于创建bean实例的合并bean定义（原始bean定义的处理副本）。
 *
 * <p>例如，{@link #postProcessMergedBeanDefinition}方法可以内省bean定义，以准备一些缓存的元数据，然后再处理bean的实例。也允许修改bean定义，但是仅限于实际上是用于并发修改的定义属性。基本上，这仅适用于{@link RootBeanDefinition}本身定义的操作，而不适用于其基类的属性。
 *
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getMergedBeanDefinition
 * @since 2.5
 */
public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {

	/**
	 * 对合并后的 RootBeanDefinition 进行后处理，用于在 bean 实例化之前对 bean 的定义进行修改。
	 *
	 * @param beanDefinition 合并后的 bean 定义
	 * @param beanType       bean 实例的实际类型
	 * @param beanName       bean 的名称
	 * @see AbstractAutowireCapableBeanFactory#applyMergedBeanDefinitionPostProcessors
	 */
	void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);

	/**
	 * 当指定 bean 的 RootBeanDefinition 被重置时，该方法会被调用，用于清除该 bean 的任何元数据。
	 * 默认实现为空。
	 *
	 * @param beanName bean 的名称
	 * @see DefaultListableBeanFactory#resetBeanDefinition
	 * @since 5.1
	 */
	default void resetBeanDefinition(String beanName) {
	}
}

