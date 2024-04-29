/*
 * Copyright 2002-2021 the original author or authors.
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

/**
 * 适用于“标准”（非Web）应用程序的{@link Environment}实现。
 * <p>
 * 除了{@link ConfigurableEnvironment}通常的功能（如属性解析和与配置文件相关的操作）之外，此实现还配置了两个默认属性源，
 * 搜索顺序如下：
 * <ul>
 * <li>{@linkplain AbstractEnvironment#getSystemProperties() 系统属性}
 * <li>{@linkplain AbstractEnvironment#getSystemEnvironment() 系统环境变量}
 * </ul>
 * <p>
 * 也就是说，如果键“xyz”同时存在于JVM系统属性和当前进程的环境变量集中，那么从调用{@code environment.getProperty("xyz")}将返回系统属性中键“xyz”的值。
 * 选择这种默认顺序是因为系统属性是针对JVM的，而环境变量可能是系统上多个JVM共有的。通过让系统属性优先，可以在JVM级别上覆盖环境变量。
 *
 * <p>可以删除、重新排序或替换这些默认属性源，并且可以使用{@link MutablePropertySources}实例通过{@link #getPropertySources()}添加额外的属性源。
 * 有关使用示例，请参见{@link ConfigurableEnvironment}的Javadoc。
 *
 * <p>请参见{@link SystemEnvironmentPropertySource}的Javadoc，了解在shell环境中（例如Bash）对不允许在变量名称中使用句点字符的属性名称进行特殊处理的详细信息。
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @see ConfigurableEnvironment
 * @see SystemEnvironmentPropertySource
 * @see org.springframework.web.context.support.StandardServletEnvironment
 * @since 3.1
 */
@SuppressWarnings("JavadocReference")
public class StandardEnvironment extends AbstractEnvironment {

	/** System environment property source name: {@value}. */
	public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";

	/** JVM system properties property source name: {@value}. */
	public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";


	/**
	 * Create a new {@code StandardEnvironment} instance with a default
	 * {@link MutablePropertySources} instance.
	 */
	public StandardEnvironment() {
	}

	/**
	 * Create a new {@code StandardEnvironment} instance with a specific
	 * {@link MutablePropertySources} instance.
	 * @param propertySources property sources to use
	 * @since 5.3.4
	 */
	protected StandardEnvironment(MutablePropertySources propertySources) {
		super(propertySources);
	}


	/**
	 * Customize the set of property sources with those appropriate for any standard
	 * Java environment:
	 * <ul>
	 * <li>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}
	 * <li>{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}
	 * </ul>
	 * <p>Properties present in {@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME} will
	 * take precedence over those in {@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}.
	 * @see AbstractEnvironment#customizePropertySources(MutablePropertySources)
	 * @see #getSystemProperties()
	 * @see #getSystemEnvironment()
	 */
	@Override
	protected void customizePropertySources(MutablePropertySources propertySources) {
		propertySources.addLast(
				new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, getSystemProperties()));
		propertySources.addLast(
				new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));
	}

}
