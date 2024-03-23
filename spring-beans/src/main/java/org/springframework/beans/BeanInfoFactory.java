/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans;

import org.springframework.lang.Nullable;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;

/**
 * 策略接口，用于为Springbeans创建{@link BeanInfo}实例。
 * 可以用于 Custom bean property resolution strategies (e.g. for other
 * languages on the JVM) or more efficient {@link BeanInfo}获取算法。
 *
 * <p>BeanInfoFactories由{@link CachedIntrospectionResults}通过使用{@link org.springframework.core.io.support.SpringFactoriesLoader}实用类创建。
 * <p>
 * 当需要创建{@link BeanInfo}时，{@code CachedIntrospectionResults}将遍历发现的工厂，调用{@link #getBeanInfo(Class)}工厂方法。
 * 如果返回{@code null}，将查询下一个工厂。如果 none of the factories support the class, a standard {@link BeanInfo}将作为默认创建。
 *
 * <p>注意，{@link org.springframework.core.io.support.SpringFactoriesLoader}对{@code BeanInfoFactory}实例进行了排序，
 * 根据{@link org.springframework.core.annotation.Order @Order}属性。
 *
 * @author Arjen Poutsma
 * @see CachedIntrospectionResults
 * @see org.springframework.core.io.support.SpringFactoriesLoader
 * @since 3.2
 */
public interface BeanInfoFactory {

	/**
	 * 返回对于给定类的BeanInfo。
	 *
	 * @param beanClass 给定类
	 * @return 对应的BeanInfo，如果支持
	 * @throws IntrospectionException 如果在获取过程中出现异常
	 */
	@Nullable
	BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException;

}
