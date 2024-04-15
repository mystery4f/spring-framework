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

package org.springframework.core.env;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Abstract base class for resolving properties against any underlying source.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractPropertyResolver implements ConfigurablePropertyResolver {

	protected final Log logger = LogFactory.getLog(getClass());
	private final Set<String> requiredProperties = new LinkedHashSet<>();
	@Nullable
	private volatile ConfigurableConversionService conversionService;
	@Nullable
	private PropertyPlaceholderHelper nonStrictHelper;
	@Nullable
	private PropertyPlaceholderHelper strictHelper;
	private boolean ignoreUnresolvableNestedPlaceholders = false;
	private String placeholderPrefix = SystemPropertyUtils.PLACEHOLDER_PREFIX;
	private String placeholderSuffix = SystemPropertyUtils.PLACEHOLDER_SUFFIX;
	@Nullable
	private String valueSeparator = SystemPropertyUtils.VALUE_SEPARATOR;

	@Override
	public ConfigurableConversionService getConversionService() {
		// Need to provide an independent DefaultConversionService, not the
		// shared DefaultConversionService used by PropertySourcesPropertyResolver.
		ConfigurableConversionService cs = this.conversionService;
		if (cs == null) {
			synchronized (this) {
				cs = this.conversionService;
				if (cs == null) {
					cs = new DefaultConversionService();
					this.conversionService = cs;
				}
			}
		}
		return cs;
	}

	@Override
	public void setConversionService(ConfigurableConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}

	/**
	 * Set the prefix that placeholders replaced by this resolver must begin with.
	 * <p>The default is "${".
	 *
	 * @see org.springframework.util.SystemPropertyUtils#PLACEHOLDER_PREFIX
	 */
	@Override
	public void setPlaceholderPrefix(String placeholderPrefix) {
		Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
		this.placeholderPrefix = placeholderPrefix;
	}

	/**
	 * Set the suffix that placeholders replaced by this resolver must end with.
	 * <p>The default is "}".
	 *
	 * @see org.springframework.util.SystemPropertyUtils#PLACEHOLDER_SUFFIX
	 */
	@Override
	public void setPlaceholderSuffix(String placeholderSuffix) {
		Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
		this.placeholderSuffix = placeholderSuffix;
	}

	/**
	 * Specify the separating character between the placeholders replaced by this
	 * resolver and their associated default value, or {@code null} if no such
	 * special character should be processed as a value separator.
	 * <p>The default is ":".
	 *
	 * @see org.springframework.util.SystemPropertyUtils#VALUE_SEPARATOR
	 */
	@Override
	public void setValueSeparator(@Nullable String valueSeparator) {
		this.valueSeparator = valueSeparator;
	}

	/**
	 * 设置是否在遇到无法解析的占位符时抛出异常。
	 * 嵌套在给定属性值中的占位符。{@code false}表示严格解析，即将抛出异常。
	 * {@code true}表示应通过未解析的嵌套占位符以其未解析的${...}形式传递。
	 * <p>默认值为{@code false}。
	 *
	 * @since 3.2
	 */
	@Override
	public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
		this.ignoreUnresolvableNestedPlaceholders = ignoreUnresolvableNestedPlaceholders;
	}


	@Override
	public void setRequiredProperties(String... requiredProperties) {
		Collections.addAll(this.requiredProperties, requiredProperties);
	}

	@Override
	public void validateRequiredProperties() {
		MissingRequiredPropertiesException ex = new MissingRequiredPropertiesException();
		for (String key : this.requiredProperties) {
			if (this.getProperty(key) == null) {
				ex.addMissingRequiredProperty(key);
			}
		}
		if (!ex.getMissingRequiredProperties().isEmpty()) {
			throw ex;
		}
	}

	@Override
	@Nullable
	public String getProperty(String key) {
		return getProperty(key, String.class);
	}

	@Override
	public boolean containsProperty(String key) {
		return (getProperty(key) != null);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		String value = getProperty(key);
		return (value != null ? value : defaultValue);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		T value = getProperty(key, targetType);
		return (value != null ? value : defaultValue);
	}

	@Override
	public String getRequiredProperty(String key) throws IllegalStateException {
		String value = getProperty(key);
		if (value == null) {
			throw new IllegalStateException("Required key '" + key + "' not found");
		}
		return value;
	}

	@Override
	public <T> T getRequiredProperty(String key, Class<T> valueType) throws IllegalStateException {
		T value = getProperty(key, valueType);
		if (value == null) {
			throw new IllegalStateException("Required key '" + key + "' not found");
		}
		return value;
	}

	@Override
	public String resolvePlaceholders(String text) {
		if (this.nonStrictHelper == null) {
			this.nonStrictHelper = createPlaceholderHelper(true);
		}
		return doResolvePlaceholders(text, this.nonStrictHelper);
	}

	@Override
	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		if (this.strictHelper == null) {
			this.strictHelper = createPlaceholderHelper(false);
		}
		return doResolvePlaceholders(text, this.strictHelper);
	}

	/**
	 * 解决给定字符串中的占位符，根据{@link #setIgnoreUnresolvableNestedPlaceholders}的值确定是否应引发异常或忽略任何无法解析的占位符。
	 * <p> 从{@link #getProperty}及其变体调用，隐式解析嵌套占位符。相比之下，{@link #resolvePlaceholders}和{@link #resolveRequiredPlaceholders}不会委托给此方法，而是根据这些方法各自指定的方式来处理无法解析的占位符。
	 *
	 * @see #setIgnoreUnresolvableNestedPlaceholders
	 * @since 3.2
	 */
	protected String resolveNestedPlaceholders(String value) {
		if (value.isEmpty()) {
			return value;
		}
		return (this.ignoreUnresolvableNestedPlaceholders ?
				resolvePlaceholders(value) : resolveRequiredPlaceholders(value));
	}

	/**
	 * 创建一个属性占位符帮助器。
	 *
	 * @param ignoreUnresolvablePlaceholders 是否忽略无法解析的占位符。
	 * @return 返回配置好的PropertyPlaceholderHelper实例。
	 */
	private PropertyPlaceholderHelper createPlaceholderHelper(boolean ignoreUnresolvablePlaceholders) {
		// 使用指定的前缀、后缀、分隔符以及是否忽略无法解析的占位符参数，创建属性占位符帮助器实例
		return new PropertyPlaceholderHelper(this.placeholderPrefix,
				this.placeholderSuffix,
				this.valueSeparator,
				ignoreUnresolvablePlaceholders
		);
	}

	/**
	 * 解析文本中的占位符。
	 *
	 * @param text   包含占位符的文本字符串。
	 * @param helper 用于解析占位符的PropertyPlaceholderHelper实例。
	 * @return 返回解析后的文本，占位符被其相应的属性值替换。
	 */
	private String doResolvePlaceholders(String text, PropertyPlaceholderHelper helper) {
		// 使用占位符帮助器替换文本中的占位符为实际的属性值
		return helper.replacePlaceholders(text, this::getPropertyAsRawString);
	}

	/**
	 * 将给定的值转换为指定的目标类型，如果有必要的话。
	 *
	 * @param value      原始属性值
	 * @param targetType 指定的属性获取目标类型
	 * @return 转换后的值，如果不需要转换则返回原始值
	 * @since 4.3.5
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected <T> T convertValueIfNecessary(Object value, @Nullable Class<T> targetType) {
		if (targetType == null) {
			return (T) value;
		}
		ConversionService conversionServiceToUse = this.conversionService;
		if (conversionServiceToUse == null) {
			// 若无须标准类型转换，则避免初始化共享的DefaultConversionService...
			if (ClassUtils.isAssignableValue(targetType, value)) {
				return (T) value;
			}
			conversionServiceToUse = DefaultConversionService.getSharedInstance();
		}
		return conversionServiceToUse.convert(value, targetType);
	}


	/**
	 * Retrieve the specified property as a raw String,
	 * i.e. without resolution of nested placeholders.
	 *
	 * @param key the property name to resolve
	 * @return the property value or {@code null} if none found
	 */
	@Nullable
	protected abstract String getPropertyAsRawString(String key);

}
