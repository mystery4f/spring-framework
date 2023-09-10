/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory;

import org.springframework.lang.Nullable;

/**
 * Sub-interface implemented by bean factories that can be part
 * of a hierarchy.
 *
 * <p>The corresponding {@code setParentBeanFactory} method for bean
 * factories that allow setting the parent in a configurable
 * fashion can be found in the ConfigurableBeanFactory interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setParentBeanFactory
 * @since 07.07.2003
 */
public interface HierarchicalBeanFactory extends BeanFactory {

	/**
	 * 返回父Bean工厂，如果没有则返回{@code null}。
	 */
	@Nullable
	BeanFactory getParentBeanFactory();

	/**
	 * 返回本地Bean工厂是否包含给定名称的Bean，
	 * 忽略在祖先上下文中定义的Bean。
	 * <p>这是一个替代方法，忽略祖先Bean工厂中给定名称的Bean。
	 *
	 * @param name 要查询的Bean的名称
	 * @return 本地工厂中是否定义了具有给定名称的Bean
	 * @see BeanFactory#containsBean
	 */
	boolean containsLocalBean(String name);

}
