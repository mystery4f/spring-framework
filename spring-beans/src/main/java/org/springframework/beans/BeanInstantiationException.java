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

package org.springframework.beans;

import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Exception thrown when instantiation of a bean failed.
 * Carries the offending bean class.
 *
 * @author Juergen Hoeller
 * @since 1.2.8
 */
@SuppressWarnings("serial")
public class BeanInstantiationException extends FatalBeanException {

	/**
	 * 存储被装饰的bean的Class对象
	 */
	private final Class<?> beanClass;

	/**
	 * 存储被装饰的bean的构造函数
	 */
	@Nullable
	private final Constructor<?> constructor;

	/**
	 * 存储被装饰的bean的构造方法
	 */
	@Nullable
	private final Method constructingMethod;


	/**
	 * Create a new BeanInstantiationException.
	 *
	 * @param beanClass the offending bean class
	 * @param msg       the detail message
	 */
	public BeanInstantiationException(Class<?> beanClass, String msg) {
		// 通过构造函数创建一个新的BeanInstantiationException，传入beanClass和msg参数
		this(beanClass, msg, null);
	}

	/**
	 * Create a new BeanInstantiationException.
	 *
	 * @param beanClass the offending bean class
	 * @param msg       the detail message
	 * @param cause     the root cause
	 */
	public BeanInstantiationException(Class<?> beanClass, String msg, @Nullable Throwable cause) {
		// 通过构造函数创建一个新的BeanInstantiationException，传入beanClass、msg和cause参数
		super("Failed to instantiate [" + beanClass.getName() + "]: " + msg, cause);
		// 设置beanClass、constructor和constructingMethod属性
		this.beanClass = beanClass;
		this.constructor = null;
		this.constructingMethod = null;
	}

	/**
	 * Create a new BeanInstantiationException.
	 *
	 * @param constructor the offending constructor
	 * @param msg         the detail message
	 * @param cause       the root cause
	 * @since 4.3
	 */
	public BeanInstantiationException(Constructor<?> constructor, String msg, @Nullable Throwable cause) {
		// 通过构造函数创建一个新的BeanInstantiationException，传入constructor、msg和cause参数
		super("Failed to instantiate [" + constructor.getDeclaringClass().getName() + "]: " + msg, cause);
		// 设置beanClass、constructor和constructingMethod属性
		this.beanClass = constructor.getDeclaringClass();
		this.constructor = constructor;
		this.constructingMethod = null;
	}

	/**
	 * Create a new BeanInstantiationException.
	 *
	 * @param constructingMethod the delegate for bean construction purposes
	 *                           (typically, but not necessarily, a static factory method)
	 * @param msg                the detail message
	 * @param cause              the root cause
	 * @since 4.3
	 */
	public BeanInstantiationException(Method constructingMethod, String msg, @Nullable Throwable cause) {
		// 通过构造函数创建一个新的BeanInstantiationException，传入constructingMethod、msg和cause参数
		super("Failed to instantiate [" + constructingMethod.getReturnType().getName() + "]: " + msg, cause);
		// 设置beanClass、constructor和constructingMethod属性
		this.beanClass = constructingMethod.getReturnType();
		this.constructor = null;
		this.constructingMethod = constructingMethod;
	}


	/**
	 * Return the offending bean class (never {@code null}).
	 *
	 * @return the class that was to be instantiated
	 */
	public Class<?> getBeanClass() {
		// 返回beanClass属性
		return this.beanClass;
	}

	/**
	 * Return the offending constructor, if known.
	 *
	 * @return the constructor in use, or {@code null} in case of a
	 * factory method or in case of default instantiation
	 * @since 4.3
	 */
	@Nullable
	public Constructor<?> getConstructor() {
		// 返回constructor属性
		return this.constructor;
	}

	/**
	 * Return the delegate for bean construction purposes, if known.
	 *
	 * @return the method in use (typically a static factory method),
	 * or {@code null} in case of constructor-based instantiation
	 * @since 4.3
	 */
	@Nullable
	public Method getConstructingMethod() {
		// 返回constructingMethod属性
		return this.constructingMethod;
	}

}
