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

package org.springframework.core.env;

import java.util.Map;

/**
 * 一种配置接口，大多数（如果不是全部）{@link Environment} 类型都需要实现它。该接口提供了设置激活和默认配置文件的设施，
 * 以及操作底层属性源的功能。通过 {@link ConfigurablePropertyResolver} 超接口，允许客户端设置和验证所需的属性，
 * 自定义转换服务等。
 *
 * <h2>操作属性源</h2>
 * <p>属性源可以被移除、重新排序或替换；并且可以通过 {@link MutablePropertySources} 实例，
 * 从 {@link #getPropertySources()} 返回，添加额外的属性源。以下示例针对的是 {@link StandardEnvironment}，
 * {@code ConfigurableEnvironment} 的实现，但一般适用于任何实现，尽管特定的默认属性源可能有所不同。
 *
 * <h4>示例：添加一个新的属性源，具有最高的搜索优先级</h4>
 * <pre class="code">
 * ConfigurableEnvironment environment = new StandardEnvironment();
 * MutablePropertySources propertySources = environment.getPropertySources();
 * Map&lt;String, String&gt; myMap = new HashMap&lt;&gt;();
 * myMap.put("xyz", "myValue");
 * propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
 * </pre>
 *
 * <h4>示例：移除默认的系统属性源</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
 * </pre>
 *
 * <h4>示例：为了测试目的模拟系统环境变量</h4>
 * <pre class="code">
 * MutablePropertySources propertySources = environment.getPropertySources();
 * MockPropertySource mockEnvVars = new MockPropertySource().withProperty("xyz", "myValue");
 * propertySources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
 * </pre>
 * <p>
 * 当一个 {@link Environment} 被 {@code ApplicationContext} 使用时，重要的是任何这样的 {@code PropertySource} 操作
 * 必须在上下文的 {@link org.springframework.context.support.AbstractApplicationContext#refresh() 刷新()} 方法被调用之前执行。
 * 这样可以确保在容器启动过程中所有属性源都可用，包括被 {@linkplain
 * org.springframework.context.support.PropertySourcesPlaceholderConfigurer 属性占位符配置器} 使用的情况。
 *
 * @author Chris Beams
 * @see StandardEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment
 * @since 3.1
 */

public interface ConfigurableEnvironment extends Environment, ConfigurablePropertyResolver {

	/**
	 * 设置此{@code Environment}激活的配置文件集。在容器启动时评估这些配置文件，以确定是否应向容器注册bean定义。
	 * <p>任何现有的激活配置文件都将被给定的参数替换；调用此方法时没有参数可以清除当前的激活配置文件集。使用
	 * {@link #addActiveProfile}来添加配置文件，同时保留现有的配置文件集。
	 *
	 * @throws IllegalArgumentException 如果任何配置文件为null、空或仅包含空白字符
	 * @see #addActiveProfile
	 * @see #setDefaultProfiles
	 * @see org.springframework.context.annotation.Profile
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	void setActiveProfiles(String... profiles);

	/**
	 * 将一个配置文件添加到当前激活配置文件的集合中。
	 *
	 * @throws IllegalArgumentException 如果配置文件为null、空或仅包含空白字符
	 * @see #setActiveProfiles
	 */
	void addActiveProfile(String profile);

	/**
	 * 指定一组默认配置文件，如果未通过{@link #setActiveProfiles}显式激活其他配置文件，则这些配置文件将被激活。
	 *
	 * @throws IllegalArgumentException 如果任何配置文件为null、空或仅包含空白字符
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 */
	void setDefaultProfiles(String... profiles);

	/**
	 * 返回此{@code Environment}的{@link PropertySources}，以可变形式允许对搜索此{@code Environment}对象时应搜索的{@link PropertySource}对象集进行操作。
	 * 各种{@link MutablePropertySources}方法，例如{@link MutablePropertySources#addFirst addFirst}、
	 * {@link MutablePropertySources#addLast addLast}、{@link MutablePropertySources#addBefore addBefore}和
	 * {@link MutablePropertySources#addAfter addAfter}允许对属性源顺序进行细粒度控制。这对于确保某些用户定义的属性源具有搜索优先权非常有用
	 * ，而不是默认属性源，例如系统属性集或系统环境变量集。
	 *
	 * @see AbstractEnvironment#customizePropertySources
	 */
	MutablePropertySources getPropertySources();

	/**
	 * 如果当前{@link SecurityManager}允许，返回{@link System#getProperties()}的值，否则返回一个映射实现，该实现将尝试使用调用{@link System#getProperty(String)}来访问单个键。
	 * <p>大多数{@code Environment}实现将此系统属性映射作为默认{@link PropertySource}来搜索。因此，除非明确打算绕过其他属性源，否则建议不直接使用此方法。
	 * <p>对返回的映射调用{@link Map#get(Object)}将永远不会抛出{@link IllegalAccessException}；在安全管理员禁止访问属性的情况下，将返回null，并记录一条注意异常的日志消息。
	 */
	Map<String, Object> getSystemProperties();

	/**
	 * 如果当前{@link SecurityManager}允许，返回{@link System#getenv()}的值，否则返回一个映射实现，该实现将尝试使用调用{@link System#getenv(String)}来访问单个键。
	 * <p>大多数{@link Environment}实现将此系统环境映射作为默认{@link PropertySource}来搜索。因此，除非明确打算绕过其他属性源，否则建议不直接使用此方法。
	 * <p>对返回的映射调用{@link Map#get(Object)}将永远不会抛出{@link IllegalAccessException}；在安全管理员禁止访问属性的情况下，将返回null，并记录一条注意异常的日志消息。
	 */
	Map<String, Object> getSystemEnvironment();

	/**
	 * 将给定父环境的激活配置文件、默认配置文件和属性源分别附加到此（子）环境的相应集合中。
	 * <p>对于父环境和子环境中都存在的同名{@code PropertySource}实例，将保留子实例并丢弃父实例。这使得子环境能够通过覆盖属性源来重写属性，
	 * 同时避免了对常见属性源类型的冗余搜索，例如系统环境和系统属性。
	 * <p>激活和默认配置文件名称也通过筛选掉重复项来避免混淆和冗余存储。
	 * <p>无论如何，父环境保持不变。请注意，任何在调用{@code merge}之后对父环境的更改都不会在子环境中反映出来。因此，应谨慎处理在调用{@code merge}之前配置父属性源和配置文件信息。
	 *
	 * @param parent 要合并的环境
	 * @see org.springframework.context.support.AbstractApplicationContext#setParent
	 * @since 3.1.2
	 */
	void merge(ConfigurableEnvironment parent);


}
