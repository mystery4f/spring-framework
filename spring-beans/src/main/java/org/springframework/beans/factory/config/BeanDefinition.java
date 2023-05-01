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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * A BeanDefinition describes a bean instance, which has property values,
 * constructor argument values, and further information supplied by
 * concrete implementations.
 *
 * <p>This is just a minimal interface: The main intention is to allow a
 * {@link BeanFactoryPostProcessor} to introspect and modify property values
 * and other bean metadata.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 19.03.2004
 * @see ConfigurableListableBeanFactory#getBeanDefinition
 * @see org.springframework.beans.factory.support.RootBeanDefinition
 * @see org.springframework.beans.factory.support.ChildBeanDefinition
 */
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {

	/**
	 * 标准单例范围的作用域标识符: {@value}。
	 * <p>注意，扩展的Bean工厂可能支持更多的范围。
	 *
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_SINGLETON
	 */
	String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

	/**
	 * 标准原型范围的作用域标识符: {@value}。
	 * <p>注意，扩展的Bean工厂可能支持更多的范围。
	 * @see #setScope
	 * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE
	 */
	String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;

	/**
	 * 角色提示，表示{@code BeanDefinition}是应用程序的重要部分。
	 * 通常对应于用户定义的bean。
	 */
	int ROLE_APPLICATION = 0;

	/**
	 * 角色提示，表示{@code BeanDefinition}是配置的某些辅助部分，
	 * 通常是外层{@link org.springframework.beans.factory.parsing.ComponentDefinition}。
	 * 当更仔细地查看特定的{@link org.springframework.beans.factory.parsing.ComponentDefinition}时，
	 * 支持bean被认为足够重要，但在查看应用程序的整体配置时不是。
	 */
	int ROLE_SUPPORT = 1;

	/**
	 * 角色提示，表示{@code BeanDefinition}提供了完全的后台角色，对最终用户没有影响。
	 * 当注册完全是{@link org.springframework.beans.factory.parsing.ComponentDefinition}内部工作的bean时，
	 * 使用此提示。
	 */
	int ROLE_INFRASTRUCTURE = 2;

	// 可修改属性

	/**
	 * 返回此bean定义的父定义的名称（如果有）。
	 */
	@Nullable
	String getParentName();

	/**
	 * 如果有，请设置此bean定义的父定义的名称。
	 */
	void setParentName(@Nullable String parentName);

	/**
	 * 返回此bean定义的当前bean类名称。
	 * <p>请注意，这不必是在运行时使用的实际类名，
	 * 如果子定义从其父项覆盖/继承类名，则在运行时使用。
	 * 此外，这可能只是调用工厂方法的类，或者甚至在调用方法时为空
	 * 在工厂bean引用上。因此，不要将其视为运行时的定义性bean类型，
	 * 而只将其用于单个bean定义级别的解析目的。
	 * @see #getParentName()
	 * @see #getFactoryBeanName()
	 * @see #getFactoryMethodName()
	 */
	@Nullable
	String getBeanClassName();

	/**
	 * 指定此bean定义的bean类名称。
	 * <p>在bean工厂后处理期间，类名可以被修改，通常用解析后的变体替换原始类名。
	 * @see #setParentName
	 * @see #setFactoryBeanName
	 * @see #setFactoryMethodName
	 */
	void setBeanClassName(@Nullable String beanClassName);

	/**
	 * 返回此bean的当前目标范围的名称，如果尚未知，则为{@code null}。
	 */
	@Nullable
	String getScope();

	/**
	 * 重写此bean的目标范围，指定新的范围名称。
	 * @see #SCOPE_SINGLETON
	 * @see #SCOPE_PROTOTYPE
	 */
	void setScope(@Nullable String scope);

	/**
	 * 返回此bean是否应该进行懒惰初始化，即不会急切地在启动时实例化。
	 * 仅适用于单例bean。
	 */
	boolean isLazyInit();

	/**
	 * 设置是否应该懒惰初始化此bean。
	 * <p>如果{@code false}，则bean将由在启动时执行单例的bean工厂实例化。
	 */
	void setLazyInit(boolean lazyInit);

	/**
	 * 返回此bean所依赖的bean的名称。
	 */
	@Nullable
	String[] getDependsOn();

	/**
	 * 设置必须先初始化的bean的名称。
	 * Bean工厂将保证这些bean首先被初始化。
	 */
	void setDependsOn(@Nullable String... dependsOn);

	/**
	 * 返回此bean是否有资格成为自动装配到其他bean中。
	 */
	boolean isAutowireCandidate();

	/**
	 * 设置此bean是否有资格成为自动装配到某些其他bean中。
	 * <p>请注意，此标志旨在仅影响基于类型的自动装配。
	 * 它不影响按名称明确引用的内容，即使指定的bean未标记为可自动装配，也将解析匹配。
	 * 因此，按名称自动装配将注入bean，如果名称匹配。
	 */
	void setAutowireCandidate(boolean autowireCandidate);

	/**
	 * 返回此bean是否为主要自动装配候选项。
	 */
	boolean isPrimary();

	/**
	 * 设置此bean是否为主要自动装配候选项。
	 * <p>如果对于多个匹配的候选项中恰好有一个bean的值为{@code true}，则它将作为决定优先级的因素。
	 */
	void setPrimary(boolean primary);

	/**
	 * 返回工厂bean名称（如果有）。
	 */
	@Nullable
	String getFactoryBeanName();

	/**
	 * 指定要使用的工厂bean（如果有）。
	 * 这是要在指定的工厂方法上调用的bean的名称。
	 * @see #setFactoryMethodName
	 */
	void setFactoryBeanName(@Nullable String factoryBeanName);

	/**
	 * 返回工厂方法（如果有）。
	 */
	@Nullable
	String getFactoryMethodName();

	/**
	 * 指定工厂方法（如果有）。将使用构造函数参数调用此方法，或者如果未指定参数，则不使用任何参数。
	 * 如果已指定工厂bean，则将在指定的工厂bean上调用该方法，否则将在本地bean类上作为静态方法调用。
	 * @see #setFactoryBeanName
	 * @see #setBeanClassName
	 */
	void setFactoryMethodName(@Nullable String factoryMethodName);

	/**
	 * 返回此bean的构造函数参数值。
	 * <p>返回的实例可以在bean工厂后处理期间修改。
	 * @return ConstructorArgumentValues对象（永远不会为{@code null}）
	 */
	ConstructorArgumentValues getConstructorArgumentValues();

	/**
	 * 返回是否为此bean定义定义了构造函数参数值。
	 * @since 5.0.2
	 */
	default boolean hasConstructorArgumentValues() {
		return !getConstructorArgumentValues().isEmpty();
	}

	/**
	 * 返回要应用于bean的新实例的属性值。
	 * <p>返回的实例可以在bean工厂后处理期间修改。
	 * @return MutablePropertyValues对象（永远不会为{@code null}）
	 */
	MutablePropertyValues getPropertyValues();

	/**
	 * 返回是否为此bean定义定义了属性值。
	 * @since 5.0.2
	 */
	default boolean hasPropertyValues() {
		return !getPropertyValues().isEmpty();
	}

	/**
	 * 返回初始化方法的名称。
	 * @since 5.1
	 */
	@Nullable
	String getInitMethodName();

	/**
	 * 设置初始化方法的名称。
	 * @since 5.1
	 */
	void setInitMethodName(@Nullable String initMethodName);

	/**
	 * 返回销毁方法的名称。
	 * @since 5.1
	 */
	@Nullable
	String getDestroyMethodName();

	/**
	 * 设置销毁方法的名称。
	 * @since 5.1
	 */
	void setDestroyMethodName(@Nullable String destroyMethodName);

	/**
	 * 获取此{@code BeanDefinition}的角色提示。角色提示
	 * 为框架以及工具提供了关于特定{@code BeanDefinition}的角色和重要性的指示。
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	int getRole();

	/**
	 * 设置此{@code BeanDefinition}的角色提示。角色提示
	 * 为框架以及工具提供了关于特定{@code BeanDefinition}的角色和重要性的指示。
	 * @since 5.1
	 * @see #ROLE_APPLICATION
	 * @see #ROLE_SUPPORT
	 * @see #ROLE_INFRASTRUCTURE
	 */
	void setRole(int role);

	/**
	 * 返回此bean定义的人类可读的描述。
	 */
	@Nullable
	String getDescription();

	/**
	 * 设置此bean定义的人类可读的描述。
	 * @since 5.1
	 */
	void setDescription(@Nullable String description);

//只读属性

	/**
	 * 基于bean类或其他特定元数据，返回此bean定义的可解析类型。
	 * <p>这通常在运行时合并的bean定义上完全解析，但不一定在配置时定义实例上解析。
	 * @return可解析类型（可能为{@link ResolvableType#NONE}）
	 * @since 5.2
	 * @see ConfigurableBeanFactory#getMergedBeanDefinition
	 */
	ResolvableType getResolvableType();

	/**
	 * 返回此是否为<b>Singleton</b>，在所有调用中返回单个共享实例。
	 * @see #SCOPE_SINGLETON
	 */
	boolean isSingleton();

	/**
	 * 返回此是否为<b>Prototype</b>，在每次调用时返回独立实例。
	 * @since 3.0
	 * @see #SCOPE_PROTOTYPE
	 */
	boolean isPrototype();

	/**
	 * 返回此bean是否为“抽象”，即不适合实例化。
	 */
	boolean isAbstract();

	/**
	 * 返回此bean定义来源的资源描述（以显示错误情况下的上下文）。
	 */
	@Nullable
	String getResourceDescription();

	/**
	 * 返回原始BeanDefinition，如果没有则返回{@code null}。
	 * <p>允许检索装饰的bean定义（如果有）。
	 * <p>请注意，此方法返回最近的发起者。通过发起者链迭代以查找用户定义的原始BeanDefinition。
	 */
	@Nullable
	BeanDefinition getOriginatingBeanDefinition();

}
