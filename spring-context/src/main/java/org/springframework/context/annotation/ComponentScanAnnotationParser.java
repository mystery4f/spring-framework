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

package org.springframework.context.annotation;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Parser for the @{@link ComponentScan} annotation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see ClassPathBeanDefinitionScanner#scan(String...)
 * @see ComponentScanBeanDefinitionParser
 * @since 3.1
 */
class ComponentScanAnnotationParser {

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	private final BeanNameGenerator beanNameGenerator;

	private final BeanDefinitionRegistry registry;


	public ComponentScanAnnotationParser(Environment environment, ResourceLoader resourceLoader,
										 BeanNameGenerator beanNameGenerator, BeanDefinitionRegistry registry) {

		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.beanNameGenerator = beanNameGenerator;
		this.registry = registry;
	}


	public Set<BeanDefinitionHolder> parse(AnnotationAttributes componentScan, String declaringClass) {
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this.registry,
				componentScan.getBoolean("useDefaultFilters"), this.environment, this.resourceLoader
		);

		// 获取 BeanNameGenerator 类的具体实现类
		Class<? extends BeanNameGenerator> generatorClass = componentScan.getClass("nameGenerator");
		// 判断是否使用继承自父类的方法
		boolean useInheritedGenerator = (BeanNameGenerator.class == generatorClass);
		// 设置 BeanNameGenerator
		scanner.setBeanNameGenerator(useInheritedGenerator ? this.beanNameGenerator :
				BeanUtils.instantiateClass(generatorClass));

		// 获取 ScopedProxyMode 的枚举值
		ScopedProxyMode scopedProxyMode = componentScan.getEnum("scopedProxy");
		// 判断 ScopedProxyMode 是否不为默认值
		if (scopedProxyMode != ScopedProxyMode.DEFAULT) {
			// 设置 ScopedProxyMode
			scanner.setScopedProxyMode(scopedProxyMode);
		} else {
			// 获取 ScopeMetadataResolver 类的具体实现类
			Class<? extends ScopeMetadataResolver> resolverClass = componentScan.getClass("scopeResolver");
			// 设置 ScopeMetadataResolver
			scanner.setScopeMetadataResolver(BeanUtils.instantiateClass(resolverClass));
		}

		// 设置资源模式
		scanner.setResourcePattern(componentScan.getString("resourcePattern"));

		// 循环遍历组件扫描的 includeFilters 注解属性
		for (AnnotationAttributes includeFilterAttributes : componentScan.getAnnotationArray("includeFilters")) {
			// 使用 TypeFilterUtils 工具类创建类型过滤器
			List<TypeFilter> typeFilters = TypeFilterUtils.createTypeFiltersFor(includeFilterAttributes,
					this.environment,
					this.resourceLoader,
					this.registry
			);
			// 循环遍历类型过滤器
			for (TypeFilter typeFilter : typeFilters) {
				// 添加到扫描器中
				scanner.addIncludeFilter(typeFilter);
			}
		}
		// 循环遍历组件扫描的 excludeFilters 注解属性
		for (AnnotationAttributes excludeFilterAttributes : componentScan.getAnnotationArray("excludeFilters")) {
			// 使用TypeFilterUtils工具类创建类型过滤器
			List<TypeFilter> typeFilters = TypeFilterUtils.createTypeFiltersFor(excludeFilterAttributes,
					this.environment,
					this.resourceLoader,
					this.registry
			);
			// 循环遍历类型过滤器
			for (TypeFilter typeFilter : typeFilters) {
				// 添加到扫描器中
				scanner.addExcludeFilter(typeFilter);
			}
		}

		// 获取 componentScan 配置中的 lazyInit 属性
		boolean lazyInit = componentScan.getBoolean("lazyInit");
		// 如果 lazyInit 为 true，则设置 scanner.getBeanDefinitionDefaults()的 lazyInit 为 true
		if (lazyInit) {
			scanner.getBeanDefinitionDefaults().setLazyInit(true);
		}

		// 创建一个 LinkedHashSet 对象，用于存储 basePackages
		Set<String> basePackages = new LinkedHashSet<>();
		// 获取 componentScan 配置中的 basePackages 属性
		String[] basePackagesArray = componentScan.getStringArray("basePackages");
		// 遍历 basePackagesArray，将每一项替换环境变量，然后使用 StringUtils.tokenizeToStringArray 方法将替换后的字符串转换为字符串数组，最后将字符串数组添加到 basePackages 中
		// 遍历基础包数组
		for (String pkg : basePackagesArray) {
			// 将基础包中的占位符替换为实际值
			String[] tokenized = StringUtils.tokenizeToStringArray(this.environment.resolvePlaceholders(pkg),
					ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS
			);
			// 将替换后的包添加到基础包列表中
			Collections.addAll(basePackages, tokenized);
		}
		// 遍历基础包类数组
		for (Class<?> clazz : componentScan.getClassArray("basePackageClasses")) {
			// 获取基础包类所在的包名
			basePackages.add(ClassUtils.getPackageName(clazz));
		}
		// 如果基础包为空，则将声明类的包名添加到基础包列表中
		if (basePackages.isEmpty()) {
			basePackages.add(ClassUtils.getPackageName(declaringClass));
		}

		// 添加一个排除过滤器，该过滤器会排除指定的类名
		scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false) {
			// 匹配给定的类名
			@Override
			protected boolean matchClassName(String className) {
				return declaringClass.equals(className);
			}
		});
		// 使用扫描器扫描指定的包，并返回扫描结果
		return scanner.doScan(StringUtils.toStringArray(basePackages));
	}

}
