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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * 为{@link Condition}实现使用的上下文信息。
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface ConditionContext {

	/**
	 * 返回 {@link BeanDefinitionRegistry}, 如果条件匹配，该注册表将持有 bean 定义。
	 *
	 * @return {@link BeanDefinitionRegistry} 实例。
	 * @throws IllegalStateException 如果没有可用的注册表（这很不常见，只会在 {@link ClassPathScanningCandidateComponentProvider} 的情况下发生）
	 */
	BeanDefinitionRegistry getRegistry();

	/**
	 * 返回如果条件匹配将持有 bean 定义的 {@link ConfigurableListableBeanFactory}，如果 bean 工厂不可用或不可转换为 {@code ConfigurableListableBeanFactory}，则返回 {@code null}。
	 *
	 * @return {@link ConfigurableListableBeanFactory} 实例或 {@code null}。
	 */
	@Nullable
	ConfigurableListableBeanFactory getBeanFactory();

	/**
	 * 返回当前应用程序运行的 {@link Environment}。
	 *
	 * @return {@link Environment} 实例。
	 */
	Environment getEnvironment();

	/**
	 * 返回当前使用的 {@link ResourceLoader}。
	 *
	 * @return {@link ResourceLoader} 实例。
	 */
	ResourceLoader getResourceLoader();

	/**
	 * 返回应用于加载额外类的 {@link ClassLoader}（只有在甚至系统 ClassLoader 也无法访问时才为 {@code null}）。
	 *
	 * @return {@link ClassLoader} 实例或 {@code null}。
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getClassLoader();

}
