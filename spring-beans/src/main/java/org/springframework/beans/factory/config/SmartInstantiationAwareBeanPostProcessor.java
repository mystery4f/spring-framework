/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;

/**
 * {@link InstantiationAwareBeanPostProcessor} 接口的扩展，添加了一个回调接口，
 * 用于预测处理中的 bean 的最终类型。
 *
 * <p><b>注意：</b>此接口是一个特殊用途的接口，主要用于框架内部使用。
 * 一般情况下，应用程序提供的后处理器应简单地实现 {@link BeanPostProcessor} 接口
 * 或者继承 {@link InstantiationAwareBeanPostProcessorAdapter} 类。
 * 即使在点发布中，也可能会向此接口添加新方法。
 *
 * @author Juergen Hoeller
 * @see InstantiationAwareBeanPostProcessorAdapter
 * @since 2.0.3
 */
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

	/**
	 * 预测经过此处理器的 {@link #postProcessBeforeInstantiation} 回调后将要返回的 bean 的类型。
	 * <p>默认实现返回 {@code null}。
	 *
	 * @param beanClass bean 的原始类
	 * @param beanName  bean 的名称
	 * @return bean 的类型，如果无法预测则返回 {@code null}
	 * @throws BeansException BeansException
	 */
	@Nullable
	default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * 确定用于给定 bean 的候选构造函数。
	 * <p>默认实现返回 {@code null}。
	 *
	 * @param beanClass bean 的原始类（从不为 {@code null}）
	 * @param beanName  bean 的名称
	 * @return 候选构造函数数组，如果未指定则返回 {@code null}
	 * @throws org.springframework.beans.BeansException 如果发生错误
	 */
	@Nullable
	default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 * 获取对指定 bean 的早期访问引用，通常用于解决循环引用的问题。
	 * <p>此回调接口让后处理器有机会提前暴露一个包装器，
	 * 即在目标 bean 实例完全初始化之前。返回的对象应相当于
	 * {@link #postProcessBeforeInitialization} 和 {@link #postProcessAfterInitialization}
	 * 否则将暴露的对象。请注意，从这个方法返回的对象将作为 bean 引用使用，
	 * 除非后处理器在这些后续回调中返回了一个不同的包装器。换句话说：
	 * 这些后续回调要么最终暴露相同的引用，要么在这些后续回调中返回原始 bean 实例
	 * （如果已经为受影响的 bean 构建了包装器，则默认会暴露为最终 bean 引用）。
	 * <p>默认实现将给定的 {@code bean} 作为-is 返回。
	 *
	 * @param bean     原始 bean 实例
	 * @param beanName bean 的名称
	 * @return 作为 bean 引用暴露的对象 （通常以传递的 bean 实例作为默认值）
	 * @throws BeansException BeansException
	 */
	default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
