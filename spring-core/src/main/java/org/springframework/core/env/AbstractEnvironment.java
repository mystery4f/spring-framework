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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.SpringProperties;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.security.AccessControlException;
import java.util.*;

/**
 * {@link Environment} 实现的抽象基类。支持保留的默认配置文件名称的概念，并通过 {@link #ACTIVE_PROFILES_PROPERTY_NAME}
 * 和 {@link #DEFAULT_PROFILES_PROPERTY_NAME} 属性来指定激活和默认的配置文件。
 *
 * <p>具体子类主要区别在于它们默认添加的 {@link PropertySource} 对象。{@code AbstractEnvironment} 默认不添加任何。
 * 子类应通过受保护的 {@link #customizePropertySources(MutablePropertySources)} 方法来贡献属性源，
 * 而客户端应使用 {@link ConfigurableEnvironment#getPropertySources()} 并操作 {@link MutablePropertySources} API 来进行自定义。
 * 有关使用示例，请参见 {@link ConfigurableEnvironment} 的 JavaDoc。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @see ConfigurableEnvironment
 * @see StandardEnvironment
 * @since 3.1
 */

public abstract class AbstractEnvironment implements ConfigurableEnvironment {

	/**
	 * 定义一个系统属性，用于指示Spring忽略系统环境变量，
	 * 即Spring永远不通过 {@link System#getenv()} 方法尝试获取此类变量。
	 * <p>默认值为 "false"，如果无法解析Spring环境属性（例如配置字符串中的占位符），
	 * 则会回退到检查系统环境变量。如果你遇到由Spring发起的 {@code getenv} 调用产生的日志警告，
	 * 例如在具有严格 SecurityManager 设置和 AccessControlExceptions 警告的 WebSphere 上，
	 * 可以考虑将此标志设置为 "true"。
	 *
	 * @see #suppressGetenvAccess()
	 */
	public static final String IGNORE_GETENV_PROPERTY_NAME = "spring.getenv.ignore";

	/**
	 * 指定激活的配置文件名称的属性名：{@value}。值可以是逗号分隔的。
	 * <p>请注意，某些shell环境（如Bash）不允许在变量名称中使用句点字符。
	 * 假设使用了Spring的 {@link SystemEnvironmentPropertySource}，此属性可以作为环境变量
	 * 通过 {@code SPRING_PROFILES_ACTIVE} 来指定。
	 *
	 * @see ConfigurableEnvironment#setActiveProfiles
	 */
	public static final String ACTIVE_PROFILES_PROPERTY_NAME = "spring.profiles.active";

	/**
	 * 指定默认激活的配置文件名称的属性名：{@value}。值可以是逗号分隔的。
	 * <p>请注意，某些shell环境（如Bash）不允许在变量名称中使用句点字符。
	 * 假设使用了Spring的 {@link SystemEnvironmentPropertySource}，此属性可以作为环境变量
	 * 通过 {@code SPRING_PROFILES_DEFAULT} 来指定。
	 *
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 */
	public static final String DEFAULT_PROFILES_PROPERTY_NAME = "spring.profiles.default";

	/**
	 * 保留的默认配置文件名称：{@value}。如果没有显式设置默认配置文件名称并且没有显式设置激活的配置文件名称，
	 * 则此配置文件会自动默认激活。
	 *
	 * @see #getReservedDefaultProfiles
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 * @see ConfigurableEnvironment#setActiveProfiles
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	protected static final String RESERVED_DEFAULT_PROFILE_NAME = "default";


	protected final Log logger = LogFactory.getLog(getClass());

	private final Set<String> activeProfiles = new LinkedHashSet<>();

	private final Set<String> defaultProfiles = new LinkedHashSet<>(getReservedDefaultProfiles());

	/**
	 * MutablePropertySources是一个可变的属性源容器，用于存储和管理属性源。属性源是Spring框架中用于提供配置属性的关键组件。
	 * 它们可以被用来从不同的来源（例如，配置文件，环境变量，系统属性等）加载配置属性，并提供给Spring应用程序使用。
	 */
	private final MutablePropertySources propertySources;

	/**
	 * ConfigurablePropertyResolver是一个可配置的属性解析器接口，提供了更灵活的方式来解析属性值。
	 * 它可以被用来从不同的属性源中解析属性值，支持占位符的解析和递归解析。这个属性解析器是高度可配置的，
	 * 允许开发者根据自己的需求定制属性解析的逻辑。
	 */
	private final ConfigurablePropertyResolver propertyResolver;


	/**
	 * Create a new {@code Environment} instance, calling back to
	 * {@link #customizePropertySources(MutablePropertySources)} during construction to
	 * allow subclasses to contribute or manipulate {@link PropertySource} instances as
	 * appropriate.
	 *
	 * @see #customizePropertySources(MutablePropertySources)
	 */
	public AbstractEnvironment() {
		this(new MutablePropertySources());
	}

	/**
	 * Create a new {@code Environment} instance with a specific
	 * {@link MutablePropertySources} instance, calling back to
	 * {@link #customizePropertySources(MutablePropertySources)} during
	 * construction to allow subclasses to contribute or manipulate
	 * {@link PropertySource} instances as appropriate.
	 *
	 * @param propertySources property sources to use
	 * @see #customizePropertySources(MutablePropertySources)
	 * @since 5.3.4
	 */
	protected AbstractEnvironment(MutablePropertySources propertySources) {
		this.propertySources = propertySources;
		this.propertyResolver = createPropertyResolver(propertySources);
		customizePropertySources(propertySources);
	}


	/**
	 * Factory method used to create the {@link ConfigurablePropertyResolver}
	 * instance used by the Environment.
	 *
	 * @see #getPropertyResolver()
	 * @since 5.3.4
	 */
	protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
		return new PropertySourcesPropertyResolver(propertySources);
	}

	/**
	 * Customize the set of {@link PropertySource} objects to be searched by this
	 * {@code Environment} during calls to {@link #getProperty(String)} and related
	 * methods.
	 *
	 * <p>Subclasses that override this method are encouraged to add property
	 * sources using {@link MutablePropertySources#addLast(PropertySource)} such that
	 * further subclasses may call {@code super.customizePropertySources()} with
	 * predictable results. For example:
	 *
	 * <pre class="code">
	 * public class Level1Environment extends AbstractEnvironment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         super.customizePropertySources(propertySources); // no-op from base class
	 *         propertySources.addLast(new PropertySourceA(...));
	 *         propertySources.addLast(new PropertySourceB(...));
	 *     }
	 * }
	 *
	 * public class Level2Environment extends Level1Environment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         super.customizePropertySources(propertySources); // add all from superclass
	 *         propertySources.addLast(new PropertySourceC(...));
	 *         propertySources.addLast(new PropertySourceD(...));
	 *     }
	 * }
	 * </pre>
	 *
	 * <p>In this arrangement, properties will be resolved against sources A, B, C, D in that
	 * order. That is to say that property source "A" has precedence over property source
	 * "D". If the {@code Level2Environment} subclass wished to give property sources C
	 * and D higher precedence than A and B, it could simply call
	 * {@code super.customizePropertySources} after, rather than before adding its own:
	 *
	 * <pre class="code">
	 * public class Level2Environment extends Level1Environment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         propertySources.addLast(new PropertySourceC(...));
	 *         propertySources.addLast(new PropertySourceD(...));
	 *         super.customizePropertySources(propertySources); // add all from superclass
	 *     }
	 * }
	 * </pre>
	 *
	 * <p>The search order is now C, D, A, B as desired.
	 *
	 * <p>Beyond these recommendations, subclasses may use any of the {@code add*},
	 * {@code remove}, or {@code replace} methods exposed by {@link MutablePropertySources}
	 * in order to create the exact arrangement of property sources desired.
	 *
	 * <p>The base implementation registers no property sources.
	 *
	 * <p>Note that clients of any {@link ConfigurableEnvironment} may further customize
	 * property sources via the {@link #getPropertySources()} accessor, typically within
	 * an {@link org.springframework.context.ApplicationContextInitializer
	 * ApplicationContextInitializer}. For example:
	 *
	 * <pre class="code">
	 * ConfigurableEnvironment env = new StandardEnvironment();
	 * env.getPropertySources().addLast(new PropertySourceX(...));
	 * </pre>
	 *
	 * <h2>A warning about instance variable access</h2>
	 * <p>Instance variables declared in subclasses and having default initial values should
	 * <em>not</em> be accessed from within this method. Due to Java object creation
	 * lifecycle constraints, any initial value will not yet be assigned when this
	 * callback is invoked by the {@link #AbstractEnvironment()} constructor, which may
	 * lead to a {@code NullPointerException} or other problems. If you need to access
	 * default values of instance variables, leave this method as a no-op and perform
	 * property source manipulation and instance variable access directly within the
	 * subclass constructor. Note that <em>assigning</em> values to instance variables is
	 * not problematic; it is only attempting to read default values that must be avoided.
	 *
	 * @see MutablePropertySources
	 * @see PropertySourcesPropertyResolver
	 * @see org.springframework.context.ApplicationContextInitializer
	 */
	protected void customizePropertySources(MutablePropertySources propertySources) {
	}

	/**
	 * Return the {@link ConfigurablePropertyResolver} being used by the
	 * {@link Environment}.
	 *
	 * @see #createPropertyResolver(MutablePropertySources)
	 * @since 5.3.4
	 */
	protected final ConfigurablePropertyResolver getPropertyResolver() {
		return this.propertyResolver;
	}

	@Override
	public String[] getActiveProfiles() {
		return StringUtils.toStringArray(doGetActiveProfiles());
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableEnvironment interface
	//---------------------------------------------------------------------

	/**
	 * Return the set of active profiles as explicitly set through
	 * {@link #setActiveProfiles} or if the current set of active profiles
	 * is empty, check for the presence of {@link #doGetActiveProfilesProperty()}
	 * and assign its value to the set of active profiles.
	 *
	 * @see #getActiveProfiles()
	 * @see #doGetActiveProfilesProperty()
	 */
	protected Set<String> doGetActiveProfiles() {
		synchronized (this.activeProfiles) {
			if (this.activeProfiles.isEmpty()) {
				String profiles = doGetActiveProfilesProperty();
				if (StringUtils.hasText(profiles)) {
					setActiveProfiles(StringUtils.commaDelimitedListToStringArray(
							StringUtils.trimAllWhitespace(profiles)));
				}
			}
			return this.activeProfiles;
		}
	}

	/**
	 * Return the property value for the active profiles.
	 *
	 * @see #ACTIVE_PROFILES_PROPERTY_NAME
	 * @since 5.3.4
	 */
	@Nullable
	protected String doGetActiveProfilesProperty() {
		return getProperty(ACTIVE_PROFILES_PROPERTY_NAME);
	}

	@Override
	@Nullable
	public String getProperty(String key) {
		return this.propertyResolver.getProperty(key);
	}

	@Override
	public void setActiveProfiles(String... profiles) {
		Assert.notNull(profiles, "Profile array must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Activating profiles " + Arrays.asList(profiles));
		}
		synchronized (this.activeProfiles) {
			this.activeProfiles.clear();
			for (String profile : profiles) {
				validateProfile(profile);
				this.activeProfiles.add(profile);
			}
		}
	}

	/**
	 * 验证给定的profile是否有效，这个方法会在将profile添加到激活或默认profile集之前被内部调用。
	 * <p>子类可以通过重写此方法来施加对profile语法的进一步限制。
	 *
	 * @param profile 待验证的profile字符串。
	 * @throws IllegalArgumentException 如果profile为null、空、仅包含空白字符，或者以profile NOT操作符(!)开头，则抛出此异常。
	 * @see #acceptsProfiles
	 * @see #addActiveProfile
	 * @see #setDefaultProfiles
	 */
	protected void validateProfile(String profile) {
		if (!StringUtils.hasText(profile)) { // 验证profile是否有文本内容
			throw new IllegalArgumentException("Invalid profile [" + profile + "]: must contain text");
		}
		if (profile.charAt(0) == '!') { // 验证profile是否以'!'开头
			throw new IllegalArgumentException("Invalid profile [" + profile + "]: must not begin with ! operator");
		}
	}

	@Override
	public void addActiveProfile(String profile) {
		if (logger.isDebugEnabled()) {
			logger.debug("Activating profile '" + profile + "'");
		}
		validateProfile(profile);
		doGetActiveProfiles();
		synchronized (this.activeProfiles) {
			this.activeProfiles.add(profile);
		}
	}

	@Override
	public String[] getDefaultProfiles() {
		return StringUtils.toStringArray(doGetDefaultProfiles());
	}

	/**
	 * Return the set of default profiles explicitly set via
	 * {@link #setDefaultProfiles(String...)} or if the current set of default profiles
	 * consists only of {@linkplain #getReservedDefaultProfiles() reserved default
	 * profiles}, then check for the presence of {@link #doGetActiveProfilesProperty()}
	 * and assign its value (if any) to the set of default profiles.
	 *
	 * @see #AbstractEnvironment()
	 * @see #getDefaultProfiles()
	 * @see #getReservedDefaultProfiles()
	 * @see #doGetDefaultProfilesProperty()
	 */
	protected Set<String> doGetDefaultProfiles() {
		synchronized (this.defaultProfiles) {
			if (this.defaultProfiles.equals(getReservedDefaultProfiles())) {
				String profiles = doGetDefaultProfilesProperty();
				if (StringUtils.hasText(profiles)) {
					setDefaultProfiles(StringUtils.commaDelimitedListToStringArray(
							StringUtils.trimAllWhitespace(profiles)));
				}
			}
			return this.defaultProfiles;
		}
	}

	/**
	 * Return the set of reserved default profile names. This implementation returns
	 * {@value #RESERVED_DEFAULT_PROFILE_NAME}. Subclasses may override in order to
	 * customize the set of reserved names.
	 *
	 * @see #RESERVED_DEFAULT_PROFILE_NAME
	 * @see #doGetDefaultProfiles()
	 */
	protected Set<String> getReservedDefaultProfiles() {
		return Collections.singleton(RESERVED_DEFAULT_PROFILE_NAME);
	}

	/**
	 * Return the property value for the default profiles.
	 *
	 * @see #DEFAULT_PROFILES_PROPERTY_NAME
	 * @since 5.3.4
	 */
	@Nullable
	protected String doGetDefaultProfilesProperty() {
		return getProperty(DEFAULT_PROFILES_PROPERTY_NAME);
	}

	/**
	 * Specify the set of profiles to be made active by default if no other profiles
	 * are explicitly made active through {@link #setActiveProfiles}.
	 * <p>Calling this method removes overrides any reserved default profiles
	 * that may have been added during construction of the environment.
	 *
	 * @see #AbstractEnvironment()
	 * @see #getReservedDefaultProfiles()
	 */
	@Override
	public void setDefaultProfiles(String... profiles) {
		Assert.notNull(profiles, "Profile array must not be null");
		synchronized (this.defaultProfiles) {
			this.defaultProfiles.clear();
			for (String profile : profiles) {
				validateProfile(profile);
				this.defaultProfiles.add(profile);
			}
		}
	}

	@Override
	@Deprecated
	public boolean acceptsProfiles(String... profiles) {
		Assert.notEmpty(profiles, "Must specify at least one profile");
		for (String profile : profiles) {
			if (StringUtils.hasLength(profile) && profile.charAt(0) == '!') {
				if (!isProfileActive(profile.substring(1))) {
					return true;
				}
			} else if (isProfileActive(profile)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return whether the given profile is active, or if active profiles are empty
	 * whether the profile should be active by default.
	 *
	 * @throws IllegalArgumentException per {@link #validateProfile(String)}
	 */
	protected boolean isProfileActive(String profile) {
		validateProfile(profile);
		Set<String> currentActiveProfiles = doGetActiveProfiles();
		return (currentActiveProfiles.contains(profile) ||
				(currentActiveProfiles.isEmpty() && doGetDefaultProfiles().contains(profile)));
	}

	@Override
	public boolean acceptsProfiles(Profiles profiles) {
		Assert.notNull(profiles, "Profiles must not be null");
		return profiles.matches(this::isProfileActive);
	}

	@Override
	public MutablePropertySources getPropertySources() {
		return this.propertySources;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map<String, Object> getSystemProperties() {
		try {
			return (Map) System.getProperties();
		} catch (AccessControlException ex) {
			return (Map) new ReadOnlySystemAttributesMap() {
				@Override
				@Nullable
				protected String getSystemAttribute(String attributeName) {
					try {
						return System.getProperty(attributeName);
					} catch (AccessControlException ex) {
						if (logger.isInfoEnabled()) {
							logger.info("Caught AccessControlException when accessing system property '" +
									attributeName + "'; its value will be returned [null]. Reason: " + ex.getMessage());
						}
						return null;
					}
				}
			};
		}
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map<String, Object> getSystemEnvironment() {
		if (suppressGetenvAccess()) {
			return Collections.emptyMap();
		}
		try {
			return (Map) System.getenv();
		} catch (AccessControlException ex) {
			return (Map) new ReadOnlySystemAttributesMap() {
				@Override
				@Nullable
				protected String getSystemAttribute(String attributeName) {
					try {
						return System.getenv(attributeName);
					} catch (AccessControlException ex) {
						if (logger.isInfoEnabled()) {
							logger.info("Caught AccessControlException when accessing system environment variable '" +
									attributeName + "'; its value will be returned [null]. Reason: " + ex.getMessage());
						}
						return null;
					}
				}
			};
		}
	}

	/**
	 * Determine whether to suppress {@link System#getenv()}/{@link System#getenv(String)}
	 * access for the purposes of {@link #getSystemEnvironment()}.
	 * <p>If this method returns {@code true}, an empty dummy Map will be used instead
	 * of the regular system environment Map, never even trying to call {@code getenv}
	 * and therefore avoiding security manager warnings (if any).
	 * <p>The default implementation checks for the "spring.getenv.ignore" system property,
	 * returning {@code true} if its value equals "true" in any case.
	 *
	 * @see #IGNORE_GETENV_PROPERTY_NAME
	 * @see SpringProperties#getFlag
	 */
	protected boolean suppressGetenvAccess() {
		return SpringProperties.getFlag(IGNORE_GETENV_PROPERTY_NAME);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurablePropertyResolver interface
	//---------------------------------------------------------------------

	@Override
	public void merge(ConfigurableEnvironment parent) {
		for (PropertySource<?> ps : parent.getPropertySources()) {
			if (!this.propertySources.contains(ps.getName())) {
				this.propertySources.addLast(ps);
			}
		}
		String[] parentActiveProfiles = parent.getActiveProfiles();
		if (!ObjectUtils.isEmpty(parentActiveProfiles)) {
			synchronized (this.activeProfiles) {
				Collections.addAll(this.activeProfiles, parentActiveProfiles);
			}
		}
		String[] parentDefaultProfiles = parent.getDefaultProfiles();
		if (!ObjectUtils.isEmpty(parentDefaultProfiles)) {
			synchronized (this.defaultProfiles) {
				this.defaultProfiles.remove(RESERVED_DEFAULT_PROFILE_NAME);
				Collections.addAll(this.defaultProfiles, parentDefaultProfiles);
			}
		}
	}

	@Override
	public ConfigurableConversionService getConversionService() {
		return this.propertyResolver.getConversionService();
	}

	@Override
	public void setConversionService(ConfigurableConversionService conversionService) {
		this.propertyResolver.setConversionService(conversionService);
	}

	@Override
	public void setPlaceholderPrefix(String placeholderPrefix) {
		this.propertyResolver.setPlaceholderPrefix(placeholderPrefix);
	}

	@Override
	public void setPlaceholderSuffix(String placeholderSuffix) {
		this.propertyResolver.setPlaceholderSuffix(placeholderSuffix);
	}

	@Override
	public void setValueSeparator(@Nullable String valueSeparator) {
		this.propertyResolver.setValueSeparator(valueSeparator);
	}

	@Override
	public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
		this.propertyResolver.setIgnoreUnresolvableNestedPlaceholders(ignoreUnresolvableNestedPlaceholders);
	}

	@Override
	public void setRequiredProperties(String... requiredProperties) {
		this.propertyResolver.setRequiredProperties(requiredProperties);
	}


	//---------------------------------------------------------------------
	// Implementation of PropertyResolver interface
	//---------------------------------------------------------------------

	@Override
	public void validateRequiredProperties() throws MissingRequiredPropertiesException {
		this.propertyResolver.validateRequiredProperties();
	}

	@Override
	public boolean containsProperty(String key) {
		return this.propertyResolver.containsProperty(key);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return this.propertyResolver.getProperty(key, defaultValue);
	}

	@Override
	@Nullable
	public <T> T getProperty(String key, Class<T> targetType) {
		return this.propertyResolver.getProperty(key, targetType);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		return this.propertyResolver.getProperty(key, targetType, defaultValue);
	}

	@Override
	public String getRequiredProperty(String key) throws IllegalStateException {
		return this.propertyResolver.getRequiredProperty(key);
	}

	@Override
	public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
		return this.propertyResolver.getRequiredProperty(key, targetType);
	}

	@Override
	public String resolvePlaceholders(String text) {
		return this.propertyResolver.resolvePlaceholders(text);
	}

	@Override
	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		return this.propertyResolver.resolveRequiredPlaceholders(text);
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " {activeProfiles=" + this.activeProfiles +
				", defaultProfiles=" + this.defaultProfiles + ", propertySources=" + this.propertySources + "}";
	}

}
