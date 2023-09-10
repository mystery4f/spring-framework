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

package org.springframework.context.annotation;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.event.DefaultEventListenerFactory;
import org.springframework.context.event.EventListenerMethodProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class that allows for convenient registration of common
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} and
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor}
 * definitions for annotation-based configuration. Also registers a common
 * {@link org.springframework.beans.factory.support.AutowireCandidateResolver}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see ContextAnnotationAutowireCandidateResolver
 * @see ConfigurationClassPostProcessor
 * @see CommonAnnotationBeanPostProcessor
 * @see org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
 * @see org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor
 * @since 2.5
 */
public abstract class AnnotationConfigUtils {

	/**
	 * 内部管理的Configuration注解处理器的Bean名称。
	 */
	public static final String CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME = "org.springframework.context.annotation.internalConfigurationAnnotationProcessor";

	/**
	 * 内部管理的用于处理Configuration类时的BeanName生成器的Bean名称。
	 * 由AnnotationConfigApplicationContext和AnnotationConfigWebApplicationContext在引导过程中设置，
	 * 以使任何自定义的名称生成策略可用于底层的ConfigurationClassPostProcessor。
	 *
	 * @since 3.1.1
	 */
	public static final String CONFIGURATION_BEAN_NAME_GENERATOR = "org.springframework.context.annotation.internalConfigurationBeanNameGenerator";

	/**
	 * 内部管理的Autowired注解处理器的Bean名称。
	 */
	public static final String AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME = "org.springframework.context.annotation.internalAutowiredAnnotationProcessor";

	/**
	 * 内部管理的Required注解处理器的Bean名称。
	 *
	 * @deprecated 自5.1起不再默认注册Required处理器
	 */
	@Deprecated
	public static final String REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME = "org.springframework.context.annotation.internalRequiredAnnotationProcessor";

	/**
	 * 内部管理的JSR-250注解处理器的Bean名称。
	 */
	public static final String COMMON_ANNOTATION_PROCESSOR_BEAN_NAME = "org.springframework.context.annotation.internalCommonAnnotationProcessor";

	/**
	 * 内部管理的JPA注解处理器的Bean名称。
	 */
	public static final String PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME = "org.springframework.context.annotation.internalPersistenceAnnotationProcessor";
	/**
	 * 内部管理的@EventListener注解处理器的Bean名称。
	 */
	public static final String EVENT_LISTENER_PROCESSOR_BEAN_NAME = "org.springframework.context.event.internalEventListenerProcessor";
	/**
	 * 内部管理的EventListenerFactory的Bean名称。
	 */
	public static final String EVENT_LISTENER_FACTORY_BEAN_NAME = "org.springframework.context.event.internalEventListenerFactory";
	private static final String PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME = "org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor";
	private static final boolean jsr250Present;

	private static final boolean jpaPresent;

	static {
		ClassLoader classLoader = AnnotationConfigUtils.class.getClassLoader();
		jsr250Present = ClassUtils.isPresent("javax.annotation.Resource", classLoader);
		jpaPresent = ClassUtils.isPresent("javax.persistence.EntityManagerFactory", classLoader) && ClassUtils.isPresent(PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME, classLoader);
	}

	/**
	 * 在给定的注册表中注册所有相关的注解后处理器。
	 *
	 * @param registry 要操作的注册表
	 */
	public static void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry) {
		registerAnnotationConfigProcessors(registry, null);
	}

	/**
	 * 在给定的注册表中注册所有相关的注解后处理器。
	 *
	 * @param registry 要操作的注册表
	 * @param source   触发此注册的配置源元素（已提取）。可以为null。
	 * @return 包含此调用实际注册的所有bean定义的BeanDefinitionHolder的集合
	 */
	public static Set<BeanDefinitionHolder> registerAnnotationConfigProcessors(BeanDefinitionRegistry registry, @Nullable Object source) {

		DefaultListableBeanFactory beanFactory = unwrapDefaultListableBeanFactory(registry);
		if (beanFactory != null) {
			if (!(beanFactory.getDependencyComparator() instanceof AnnotationAwareOrderComparator)) {
				beanFactory.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
			}
			if (!(beanFactory.getAutowireCandidateResolver() instanceof ContextAnnotationAutowireCandidateResolver)) {
				beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
			}
		}

		Set<BeanDefinitionHolder> beanDefs = new LinkedHashSet<>(8);

		if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		if (!registry.containsBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		// 检查是否支持JSR-250，如果存在，则添加CommonAnnotationBeanPostProcessor。
		if (jsr250Present && !registry.containsBeanDefinition(COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		// 检查是否支持JPA，如果存在，则添加PersistenceAnnotationBeanPostProcessor。
		if (jpaPresent && !registry.containsBeanDefinition(PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition();
			try {
				def.setBeanClass(ClassUtils.forName(PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME, AnnotationConfigUtils.class.getClassLoader()));
			} catch (ClassNotFoundException ex) {
				throw new IllegalStateException("无法加载可选框架类：" + PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME, ex);
			}
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		if (!registry.containsBeanDefinition(EVENT_LISTENER_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition(EventListenerMethodProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_PROCESSOR_BEAN_NAME));
		}

		if (!registry.containsBeanDefinition(EVENT_LISTENER_FACTORY_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition(DefaultEventListenerFactory.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_FACTORY_BEAN_NAME));
		}

		return beanDefs;
	}

	/**
	 * 获取DefaultListableBeanFactory对象
	 *
	 * @param registry BeanDefinitionRegistry对象
	 * @return DefaultListableBeanFactory对象，如果registry不是DefaultListableBeanFactory或GenericApplicationContext类型，则返回null
	 */
	@Nullable
	private static DefaultListableBeanFactory unwrapDefaultListableBeanFactory(BeanDefinitionRegistry registry) {
		if (registry instanceof DefaultListableBeanFactory) {
			return (DefaultListableBeanFactory) registry;
		} else if (registry instanceof GenericApplicationContext) {
			return ((GenericApplicationContext) registry).getDefaultListableBeanFactory();
		} else {
			return null;
		}
	}

	/**
	 * 注册后处理器
	 *
	 * @param registry   BeanDefinitionRegistry对象
	 * @param definition RootBeanDefinition对象
	 * @param beanName   Bean的名称
	 * @return BeanDefinitionHolder对象
	 */
	private static BeanDefinitionHolder registerPostProcessor(BeanDefinitionRegistry registry, RootBeanDefinition definition, String beanName) {
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(beanName, definition);
		return new BeanDefinitionHolder(definition, beanName);
	}

	/**
	 * 处理常见的定义注解
	 *
	 * @param abd AnnotatedBeanDefinition对象
	 */
	public static void processCommonDefinitionAnnotations(AnnotatedBeanDefinition abd) {
		processCommonDefinitionAnnotations(abd, abd.getMetadata());
	}

	/**
	 * 处理常见的定义注解
	 *
	 * @param abd      带有注解的Bean定义
	 * @param metadata 注解类型的元数据
	 */
	static void processCommonDefinitionAnnotations(AnnotatedBeanDefinition abd, AnnotatedTypeMetadata metadata) {
		// 获取Lazy注解的属性
		AnnotationAttributes lazy = attributesFor(metadata, Lazy.class);
		if (lazy != null) {
			// 设置是否懒加载
			abd.setLazyInit(lazy.getBoolean("value"));
		} else if (abd.getMetadata() != metadata) {
			// 如果abd的元数据和传入的元数据不一致，则再次尝试获取Lazy注解的属性
			lazy = attributesFor(abd.getMetadata(), Lazy.class);
			if (lazy != null) {
				// 设置是否懒加载
				abd.setLazyInit(lazy.getBoolean("value"));
			}
		}

		// 判断是否有Primary注解
		if (metadata.isAnnotated(Primary.class.getName())) {
			// 设置为主要的Bean定义
			abd.setPrimary(true);
		}

		// 获取DependsOn注解的属性
		AnnotationAttributes dependsOn = attributesFor(metadata, DependsOn.class);
		if (dependsOn != null) {
			// 设置依赖的Bean名称数组
			abd.setDependsOn(dependsOn.getStringArray("value"));
		}

		// 获取Role注解的属性
		AnnotationAttributes role = attributesFor(metadata, Role.class);
		if (role != null) {
			// 设置角色
			abd.setRole(role.getNumber("value").intValue());
		}

		// 获取Description注解的属性
		AnnotationAttributes description = attributesFor(metadata, Description.class);
		if (description != null) {
			// 设置描述
			abd.setDescription(description.getString("value"));
		}
	}

	/**
	 * 根据注解类获取注解属性
	 *
	 * @param metadata        注解类型的元数据
	 * @param annotationClass 注解类
	 * @return 注解属性
	 */
	@Nullable
	static AnnotationAttributes attributesFor(AnnotatedTypeMetadata metadata, Class<?> annotationClass) {
		return attributesFor(metadata, annotationClass.getName());
	}

	/**
	 * 根据注解类名获取注解属性
	 *
	 * @param metadata            注解类型的元数据
	 * @param annotationClassName 注解类名
	 * @return 注解属性
	 */
	@Nullable
	static AnnotationAttributes attributesFor(AnnotatedTypeMetadata metadata, String annotationClassName) {
		return AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(annotationClassName, false));
	}

	/**
	 * 应用作用域代理模式
	 *
	 * @param metadata   作用域元数据
	 * @param definition Bean定义的持有者
	 * @param registry   Bean定义的注册表
	 * @return 经过作用域代理模式处理后的Bean定义的持有者
	 */
	static BeanDefinitionHolder applyScopedProxyMode(ScopeMetadata metadata, BeanDefinitionHolder definition, BeanDefinitionRegistry registry) {
		// 获取作用域代理模式
		ScopedProxyMode scopedProxyMode = metadata.getScopedProxyMode();
		if (scopedProxyMode.equals(ScopedProxyMode.NO)) {
			// 如果作用域代理模式为NO，则直接返回原始的Bean定义的持有者
			return definition;
		}
		boolean proxyTargetClass = scopedProxyMode.equals(ScopedProxyMode.TARGET_CLASS);
		// 创建作用域代理
		return ScopedProxyCreator.createScopedProxy(definition, registry, proxyTargetClass);
	}

	/**
	 * 获取可重复注解的属性集合
	 *
	 * @param metadata        注解类型的元数据
	 * @param containerClass  容器类
	 * @param annotationClass 注解类
	 * @return 可重复注解的属性集合
	 */
	static Set<AnnotationAttributes> attributesForRepeatable(AnnotationMetadata metadata, Class<?> containerClass, Class<?> annotationClass) {
		return attributesForRepeatable(metadata, containerClass.getName(), annotationClass.getName());
	}

	/**
	 * 根据注解元数据、容器类名和注解类名生成可重复注解的属性集合。
	 *
	 * @param metadata            注解元数据
	 * @param containerClassName  容器类名
	 * @param annotationClassName 注解类名
	 * @return 可重复注解的属性集合
	 */
	@SuppressWarnings("unchecked")
	static Set<AnnotationAttributes> attributesForRepeatable(AnnotationMetadata metadata, String containerClassName, String annotationClassName) {

		Set<AnnotationAttributes> result = new LinkedHashSet<>();

		// 是否直接存在注解？
		addAttributesIfNotNull(result, metadata.getAnnotationAttributes(annotationClassName, false));

		// 是否存在容器注解？
		Map<String, Object> container = metadata.getAnnotationAttributes(containerClassName, false);
		if (container != null && container.containsKey("value")) {
			for (Map<String, Object> containedAttributes : (Map<String, Object>[]) container.get("value")) {
				addAttributesIfNotNull(result, containedAttributes);
			}
		}

		// 返回合并后的结果
		return Collections.unmodifiableSet(result);
	}

	/**
	 * 如果属性集合不为空，则将其添加到结果集合中。
	 *
	 * @param result     结果集合
	 * @param attributes 属性集合
	 */
	private static void addAttributesIfNotNull(Set<AnnotationAttributes> result, @Nullable Map<String, Object> attributes) {

		if (attributes != null) {
			result.add(AnnotationAttributes.fromMap(attributes));
		}
	}

}
