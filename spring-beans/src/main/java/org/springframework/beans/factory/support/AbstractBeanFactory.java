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

package org.springframework.beans.factory.support;

import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.log.LogMessage;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.beans.PropertyEditor;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * {@link org.springframework.beans.factory.BeanFactory} 实现的抽象基类，
 * 提供了全面的 {@link org.springframework.beans.factory.config.ConfigurableBeanFactory} SPI 功能。
 * 不假定可枚举 Bean 工厂：因此也可以用作从某些后端资源获取 Bean 定义的 Bean 工厂实现的基类
 * （其中 Bean 定义访问是一个昂贵的操作）。
 * <p>此类提供了单例缓存（通过其基类 {@link org.springframework.beans.factory.support.DefaultSingletonBeanRegistry}），
 * 单例/原型确定、{@link org.springframework.beans.factory.FactoryBean} 处理、别名、子 Bean 定义的 Bean 定义合并，
 * 以及 Bean 销毁（{@link org.springframework.beans.factory.DisposableBean} 接口、自定义销毁方法）。
 * 此外，它可以通过实现 {@link org.springframework.beans.factory.HierarchicalBeanFactory} 接口来管理 Bean 工厂层次结构
 * （在未知 Bean 的情况下委托给父级）。
 * <p>子类需要实现的主要模板方法是 {@link #getBeanDefinition} 和 {@link #createBean}，
 * 分别为给定的 Bean 名称检索 Bean 定义并创建给定的 Bean 定义的 Bean 实例。
 * 这些操作的默认实现可以在 {@link DefaultListableBeanFactory} 和 {@link AbstractAutowireCapableBeanFactory} 中找到。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @see #getBeanDefinition
 * @see #createBean
 * @see AbstractAutowireCapableBeanFactory#createBean
 * @see DefaultListableBeanFactory#getBeanDefinition
 * @since 15 April 2001
 */
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

	/**
	 * CustomPropertyEditorRegistrar集合，用于应用于此工厂bean。
	 */
	private final Set<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<>(4);
	/**
	 * Custom PropertyEditors映射，用于应用于此工厂bean的bean。
	 */
	private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>(4);
	/**
	 * String解析器集合，用于解析注解属性值等。
	 */
	private final List<StringValueResolver> embeddedValueResolvers = new CopyOnWriteArrayList<>();
	/**
	 * 应用的BeanPostProcessor集合。
	 */
	private final List<BeanPostProcessor> beanPostProcessors = new BeanPostProcessorCacheAwareList();
	/**
	 * 作用域对象的Map，从范围身份标识字符串映射至Scope对象实例。
	 */
	private final Map<String, Scope> scopes = new LinkedHashMap<>(8);
	/**
	 * 已合并的RootBeanDefinition的Map，从bean名称映射至相应的合并RootBeanDefinition。
	 */
	private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256);
	/**
	 * 至少已创建一次的bean名称的集合。
	 */
	private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256));
	/**
	 * 正在创建中的bean名称集合。
	 */
	private final ThreadLocal<Object> prototypesCurrentlyInCreation = new NamedThreadLocal<>("Prototype beans currently in creation");
	/**
	 * 父BeanFactory，用于支持bean继承。
	 */
	@Nullable
	private BeanFactory parentBeanFactory;
	/**
	 * 用于解析bean类名的ClassLoader，如果需要的话。
	 */
	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();
	/**
	 * 临时用于解析bean类名的ClassLoader，如果需要的话。
	 */
	@Nullable
	private ClassLoader tempClassLoader;
	/**
	 * 是否缓存bean元数据，或对每个访问重新获取元数据。
	 */
	private boolean cacheBeanMetadata = true;
	/**
	 * 表达式解析器用于解析bean定义值中的表达式。
	 */
	@Nullable
	private BeanExpressionResolver beanExpressionResolver;
	/**
	 * 要使用的Spring ConversionService，而非PropertyEditors。
	 */
	@Nullable
	private ConversionService conversionService;
	/**
	 * 要使用的自定义TypeConverter，覆盖默认的PropertyEditor机制。
	 */
	@Nullable
	private TypeConverter typeConverter;
	/**
	 * 预过滤的BeanPostProcessor缓存。
	 */
	@Nullable
	private volatile BeanPostProcessorCache beanPostProcessorCache;
	/**
	 * 在使用SecurityManager运行时使用的安全上下文。
	 */
	@Nullable
	private SecurityContextProvider securityContextProvider;
	/**
	 * 应用启动度量。
	 */
	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

	/**
	 * Create a new AbstractBeanFactory.
	 */
	public AbstractBeanFactory() {
	}

	/**
	 * Create a new AbstractBeanFactory with the given parent.
	 *
	 * @param parentBeanFactory parent bean factory, or {@code null} if none
	 * @see #getBean
	 */
	public AbstractBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		this.parentBeanFactory = parentBeanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		return doGetBean(name, null, null, false);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return doGetBean(name, requiredType, null, false);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		return doGetBean(name, null, args, false);
	}

	/**
	 * Return an instance, which may be shared or independent, of the specified bean.
	 *
	 * @param name         the name of the bean to retrieve
	 * @param requiredType the required type of the bean to retrieve
	 * @param args         arguments to use when creating a bean instance using explicit arguments
	 *                     (only applied when creating a new instance as opposed to retrieving an existing one)
	 * @return an instance of the bean
	 * @throws BeansException if the bean could not be created
	 */
	public <T> T getBean(String name, @Nullable Class<T> requiredType, @Nullable Object... args)
			throws BeansException {

		return doGetBean(name, requiredType, args, false);
	}


	/**
	 * 根据给定的bean名称从容器中获取指定类型的bean实例，如果要获取的bean不存在，会根据BeanDefinition创建新的bean实例。
	 *
	 * @param name          bean的名称
	 * @param requiredType  需要的bean类型
	 * @param args          构造函数的参数，如果bean定义中没有指定则为null
	 * @param typeCheckOnly 是否只是检查bean类型而不是实例化bean（仅用于依赖项查找）
	 * @param <T>           bean的类型
	 * @return 如果存在则获取指定的bean实例，否则返回null
	 * @throws BeansException 当bean实例化失败时抛出
	 */
	protected <T> T doGetBean(
			String name, @Nullable Class<T> requiredType,
			@Nullable Object[] args, boolean typeCheckOnly) throws BeansException {

		// 将bean名称转换为规范的bean名称
		String beanName = transformedBeanName(name);
		Object beanInstance;

		// ****************************************************************************************************
		// 从单例缓存中获取bean实例，如果存在，则使用缓存实例。否则，创建新BeanInstance。
		// ****************************************************************************************************
		Object sharedInstance = getSingleton(beanName);
		if (sharedInstance != null && args == null) {
			// 如果实例已存在，并且构造函数参数为空，则直接返回实例
			if (logger.isTraceEnabled()) {
				if (isSingletonCurrentlyInCreation(beanName)) {
					logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
							"' that is not fully initialized yet - a consequence of a circular reference");
				} else {
					logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
				}
			}
			beanInstance = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		} else {
			// 如果实例不存在，则尝试创建实例

			// 如果当前bean实例是原型作用域的新实例，则在父BeanFactory中不递归查找（因为原型bean不是共享的）
			if (isPrototypeCurrentlyInCreation(beanName)) {
				// 如果发现当前bean实例正在创建过程中，说明可能存在循环依赖，抛出异常
				throw new BeanCurrentlyInCreationException(beanName);
			}

			// 检查父容器是否有该bean定义
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// 如果在本地BeanFactory中找不到，则尝试从父BeanFactory中获取
				String nameToLookup = originalBeanName(name);
				if (parentBeanFactory instanceof AbstractBeanFactory) {
					// 如果父BeanFactory是AbstractBeanFactory，则递归在父BeanFactory中查找
					return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
							nameToLookup, requiredType, args, typeCheckOnly);
				} else if (args != null) {
					// 如果有构造函数参数，则在父BeanFactory中获取
					return (T) parentBeanFactory.getBean(nameToLookup, args);
				} else if (requiredType != null) {
					// 如果没有构造函数参数，则使用标准的getBean方法在父BeanFactory中获取
					return parentBeanFactory.getBean(nameToLookup, requiredType);
				} else {
					// 如果既没有构造函数参数，也没有指定需要的bean类型，则在父BeanFactory中获取
					return (T) parentBeanFactory.getBean(nameToLookup);
				}
			}

			// 标记当前bean实例已创建
			if (!typeCheckOnly) {
				markBeanAsCreated(beanName);
			}

			StartupStep beanCreation = this.applicationStartup.start("spring.beans.instantiate")
					.tag("beanName", name);
			try {
				if (requiredType != null) {
					beanCreation.tag("beanType", requiredType::toString);
				}

				// 获取与给定bean名称相应的MergedBeanDefinition
				RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				// 检查MergedBeanDefinition中的各个属性是否正确
				checkMergedBeanDefinition(mbd, beanName, args);

				// 如果需要，保证当前bean的依赖递归注册
				String[] dependsOn = mbd.getDependsOn();
				if (dependsOn != null) {
					for (String dep : dependsOn) {
						if (isDependent(beanName, dep)) {
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
						}
						// 注册当前bean的依赖
						registerDependentBean(dep, beanName);
						try {
							// 实例化当前bean的依赖项
							getBean(dep);
						} catch (NoSuchBeanDefinitionException ex) {
							// 如果找不到依赖项，则抛出异常
							throw new BeanCreationException(mbd.getResourceDescription(), beanName,
									"'" + beanName + "' depends on missing bean '" + dep + "'", ex);
						}
					}
				}

				// 根据MergedBeanDefinition中的数据创建bean实例
				if (mbd.isSingleton()) {
					// 如果bean是单例作用域的，则创建单例实例
					// 如果单例实例已存在，则直接返回缓存实例，否则创建并缓存实例
					sharedInstance = getSingleton(beanName, () -> {
						try {
							return createBean(beanName, mbd, args);
						} catch (BeansException ex) {
							// 如果实例化bean失败，则从单例缓存中移除该实例（如果此前已经 eagerly 加入到缓存中）
							// 并销毁任何临时引用到该bean的所有bean实例
							destroySingleton(beanName);
							throw ex;
						}
					});
					beanInstance = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
				} else if (mbd.isPrototype()) {
					// 如果bean是原型作用域的，则每次依据BeanDefinition创建一个实例
					// 在创建完成后会将其registerSingleton， 贴上"prototype"作用域的标签
					Object prototypeInstance = null;
					try {
						beforePrototypeCreation(beanName);
						prototypeInstance = createBean(beanName, mbd, args);
					} finally {
						afterPrototypeCreation(beanName);
					}
					beanInstance = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
				} else {
					// 如果bean是自定义的作用域，则根据bean定义的作用域获取实例
					String scopeName = mbd.getScope();
					if (!StringUtils.hasLength(scopeName)) {
						throw new IllegalStateException("No scope name defined for bean '" + beanName + "'");
					}
					Scope scope = this.scopes.get(scopeName);
					if (scope == null) {
						throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
					}
					try {
						Object scopedInstance = scope.get(beanName, () -> {
							beforePrototypeCreation(beanName);
							try {
								return createBean(beanName, mbd, args);
							} finally {
								afterPrototypeCreation(beanName);
							}
						});
						beanInstance = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
					} catch (IllegalStateException ex) {
						throw new ScopeNotActiveException(beanName, scopeName, ex);
					}
				}
			} catch (BeansException ex) {
				// bean实例化失败抛出异常，记录相关异常信息
				beanCreation.tag("exception", ex.getClass().toString());
				beanCreation.tag("message", String.valueOf(ex.getMessage()));
				// 实例化bean失败了，需要进行清理
				cleanupAfterBeanCreationFailure(beanName);
				throw ex;
			} finally {
				// 结束创建bean的阶段
				beanCreation.end();
			}
		}

		// 对获取到的bean实例进行类型适配和验证，并返回对应类型的方法调用结束
		return adaptBeanInstance(name, beanInstance, requiredType);
	}

	@SuppressWarnings("unchecked")
	<T> T adaptBeanInstance(String name, Object bean, @Nullable Class<?> requiredType) {
		// 检查所需类型是否与实际 bean 实例的类型匹配。
		if (requiredType != null && !requiredType.isInstance(bean)) {
			try {
				Object convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
				if (convertedBean == null) {
					throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
				}
				return (T) convertedBean;
			} catch (TypeMismatchException ex) {
				if (logger.isTraceEnabled()) {
					logger.trace("Failed to convert bean '" + name + "' to required type '" +
							ClassUtils.getQualifiedName(requiredType) + "'", ex);
				}
				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
			}
		}
		return (T) bean;
	}

	@Override
	public boolean containsBean(String name) {
		String beanName = transformedBeanName(name);
		if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
		}
		// 未找到 -> 检查父级。
		BeanFactory parentBeanFactory = getParentBeanFactory();
		return (parentBeanFactory != null && parentBeanFactory.containsBean(originalBeanName(name)));
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		// 将给定的bean名称转换为形式化的名称
		String beanName = transformedBeanName(name);

		// 获取给定名称的单例实例对象（如果存在）
		Object beanInstance = getSingleton(beanName, false);
		// 如果单例实例对象不为空
		if (beanInstance != null) {
			// 如果单例实例对象是FactoryBean类型
			if (beanInstance instanceof FactoryBean) {
				// 如果给定名称是FactoryDereference，或者单例实例对象是单例模式，则返回true
				return (BeanFactoryUtils.isFactoryDereference(name) || ((FactoryBean<?>) beanInstance).isSingleton());
			} else {
				// 如果给定名称不是FactoryDereference，则返回true
				return !BeanFactoryUtils.isFactoryDereference(name);
			}
		}

		// 如果未找到给定名称的单例实例对象，则检查bean定义
		BeanFactory parentBeanFactory = getParentBeanFactory();
		// 如果此工厂中没有找到bean定义，则委托给父BeanFactory
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			return parentBeanFactory.isSingleton(originalBeanName(name));
		}

		// 获取合并的本地bean定义
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

		// 如果bean定义是单例模式
		if (mbd.isSingleton()) {
			// 如果bean实例是FactoryBean类型
			if (isFactoryBean(beanName, mbd)) {
				// 如果给定名称是FactoryDereference，则返回true
				if (BeanFactoryUtils.isFactoryDereference(name)) {
					return true;
				}
				// 获取FactoryBean的实例对象，并返回其是否为单例模式
				FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
				return factoryBean.isSingleton();
			} else {
				// 如果bean实例不是FactoryBean类型，则返回true
				return !BeanFactoryUtils.isFactoryDereference(name);
			}
		} else {
			// 如果bean定义不是单例模式，则返回false
			return false;
		}
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {

		// 将bean名称转换为指定格式
		String beanName = transformedBeanName(name);

		// 获取当前工厂的父工厂
		BeanFactory parentBeanFactory = getParentBeanFactory();

		// 如果当前工厂不包含指定名称的bean，则代理给父工厂进行判断
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			return parentBeanFactory.isPrototype(originalBeanName(name));
		}

		// 获取指定名称bean的合成bean定义信息
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

		// 如果该bean为原型bean，则根据情况返回true或false
		if (mbd.isPrototype()) {
			// 如果不是FactoryBean，则直接返回true；如果是FactoryBean，则需要进一步判断它所创建对象的实例类型
			return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName, mbd));
		}

		// 如果该bean不是原型bean，则返回false
		// 如果该bean是FactoryBean，则还需进一步判断它所创建对象的实例类型
		if (BeanFactoryUtils.isFactoryDereference(name)) {
			return false;
		}
		if (isFactoryBean(beanName, mbd)) {
			FactoryBean<?> fb = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
			if (System.getSecurityManager() != null) {
				// 如果有安全管理器，则通过doPrivileged()方法执行判断原型bean的操作
				return AccessController.doPrivileged(
						(PrivilegedAction<Boolean>) () ->
								((fb instanceof SmartFactoryBean && ((SmartFactoryBean<?>) fb).isPrototype()) ||
										!fb.isSingleton()),
						getAccessControlContext());
			} else {
				return ((fb instanceof SmartFactoryBean && ((SmartFactoryBean<?>) fb).isPrototype()) ||
						!fb.isSingleton());
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		return isTypeMatch(name, typeToMatch, true);
	}

	/**
	 * 这是一个BeanFactory中的方法，用于检查给定的bean名称和类类型是否匹配，同时应用额外的约束条件以确保不会提早创建bean
	 *
	 * @param name                 要查询的bean的名称
	 * @param typeToMatch          要匹配的类类型(作为ResolvableType)
	 * @param allowFactoryBeanInit 是否允许初始化FactoryBean
	 * @return 如果bean类型匹配，则为true；如果不匹配或不能确定，则为false
	 * @throws NoSuchBeanDefinitionException 如果没有找到匹配的bean
	 * @see #getBean
	 * @see #getType
	 * @since 5.2
	 */
	protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		//将转换后的bean名称存储
		String beanName = transformedBeanName(name);

		//是否是工厂引用，如是则更新标记
		boolean isFactoryDereference = BeanFactoryUtils.isFactoryDereference(name);

		// 手动检查注册单例
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
			// 如果是工厂bean，则检查类型是否匹配，并返回结果
			if (beanInstance instanceof FactoryBean) {
				if (!isFactoryDereference) {
					Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
					return (type != null && typeToMatch.isAssignableFrom(type));
				} else {
					return typeToMatch.isInstance(beanInstance);
				}
			} else if (!isFactoryDereference) {
				if (typeToMatch.isInstance(beanInstance)) {
					// 直接匹配公开的实例？
					return true;
				} else if (typeToMatch.hasGenerics() && containsBeanDefinition(beanName)) {
					// 通配符可能只能匹配目标类，而不是代理类...
					RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
					Class<?> targetType = mbd.getTargetType();
					if (targetType != null && targetType != ClassUtils.getUserClass(beanInstance)) {
						Class<?> classToMatch = typeToMatch.resolve();
						if (classToMatch != null && !classToMatch.isInstance(beanInstance)) {
							return false;
						}
						if (typeToMatch.isAssignableFrom(targetType)) {
							return true;
						}
					}
					ResolvableType resolvableType = mbd.targetType;
					if (resolvableType == null) {
						resolvableType = mbd.factoryMethodReturnType;
					}
					return (resolvableType != null && typeToMatch.isAssignableFrom(resolvableType));
				}
			}
			return false;
		} else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			// 注册的空实例
			return false;
		}

		//找不到单例实例->检查bean定义
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			//在这个工厂中找不到bean定义->委托给父级
			return parentBeanFactory.isTypeMatch(originalBeanName(name), typeToMatch);
		}

		//检索对应的bean定义。
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();

		//设置要匹配的类型
		Class<?> classToMatch = typeToMatch.resolve();
		if (classToMatch == null) {
			classToMatch = FactoryBean.class;
		}
		Class<?>[] typesToMatch = (FactoryBean.class == classToMatch ?
				new Class<?>[]{classToMatch} : new Class<?>[]{FactoryBean.class, classToMatch});

		//尝试预测bean类型
		Class<?> predictedType = null;

		// 我们正在寻找常规引用，但我们是一个具有装饰bean定义的工厂bean。目标bean应该与FactoryBean最终返回的类型相同。
		if (!isFactoryDereference && dbd != null && isFactoryBean(beanName, mbd)) {
			//  仅在用户将延迟初始化 设置为true 并且我们知道合并后的bean定义是工厂bean时才执行。
			if (!mbd.isLazyInit() || allowFactoryBeanInit) {
				RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
				Class<?> targetType = predictBeanType(dbd.getBeanName(), tbd, typesToMatch);
				if (targetType != null && !FactoryBean.class.isAssignableFrom(targetType)) {
					predictedType = targetType;
				}
			}
		}

		// 如果我们无法使用目标类型，则尝试常规预测。
		if (predictedType == null) {
			predictedType = predictBeanType(beanName, mbd, typesToMatch);
			if (predictedType == null) {
				return false;
			}
		}

		// 试图获取bean的实际ResolvableType
		ResolvableType beanType = null;

		// 如果它是FactoryBean，我们希望查看它创建的内容，而不是工厂类。
		if (FactoryBean.class.isAssignableFrom(predictedType)) {
			if (beanInstance == null && !isFactoryDereference) {
				beanType = getTypeForFactoryBean(beanName, mbd, allowFactoryBeanInit);
				predictedType = beanType.resolve();
				if (predictedType == null) {
					return false;
				}
			}
		} else if (isFactoryDereference) {
			predictedType = predictBeanType(beanName, mbd, FactoryBean.class);
			if (predictedType == null || !FactoryBean.class.isAssignableFrom(predictedType)) {
				return false;
			}
		}

		// 如果没有确切的类型，则使用bean定义目标类型或工厂方法返回类型与预测的类型匹配。
		if (beanType == null) {
			ResolvableType definedType = mbd.targetType;
			if (definedType == null) {
				definedType = mbd.factoryMethodReturnType;
			}
			if (definedType != null && definedType.resolve() == predictedType) {
				beanType = definedType;
			}
		}

		// 如果存在bean类型，则使用bean类型使通配符考虑在内
		if (beanType != null) {
			return typeToMatch.isAssignableFrom(beanType);
		}

		// 如果不存在bean类型，则降级到预测类型上
		return typeToMatch.isAssignableFrom(predictedType);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		return isTypeMatch(name, ResolvableType.forRawClass(typeToMatch));
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return getType(name, true);
	}

	// 如果 name 对应的 Bean 存在，获取该 Bean 的类型；否则尝试在父 BeanFactory 中查找
	// allowFactoryBeanInit 代表是否允许工厂 Bean 初始化
	@Override
	@Nullable
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		// 获取规范化后的 beanName（去掉 & 前缀和 FactoryBean 后缀）
		String beanName = transformedBeanName(name);

		// 尝试从当前 BeanFactory 中获取 singleton 实例
		Object beanInstance = getSingleton(beanName, false);
		if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
			// 如果该 singleton 实例存在且不是 NullBean，则返回该实例的类型信息
			if (beanInstance instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
				// 如果 beanInstance 是 FactoryBean 类型，则返回该 FactoryBean 创建的 bean 的类型信息
				return getTypeForFactoryBean((FactoryBean<?>) beanInstance);
			} else {
				// 否则，返回 beanInstance 的 Class 对象
				return beanInstance.getClass();
			}
		}

		// 如果在当前 BeanFactory 中没有查找到该 singleton 实例，则从父 BeanFactory 中查找
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
			// 如果 BeanFactory 存在 && 当前 BeanFactory 中不存在该 Bean 的 beanDefinition，则递归查询父 BeanFactory
			return parentBeanFactory.getType(originalBeanName(name));
		}

		// 从当前 BeanFactory 中获取合并后的 beanDefinition
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

		// 如果该 Bean 是被修饰的 Bean，则获取被修饰的 Bean 的类型信息
		BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
		if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
			RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
			Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd);
			if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
				return targetClass;
			}
		}

		// 获取当前 Bean 的原始类型信息（即 predictBeanType(beanName, mbd)）
		Class<?> beanClass = predictBeanType(beanName, mbd);

		// 如果当前 Bean 是 FactoryBean 实例，需要获取其创建的 Bean 的类型信息
		if (beanClass != null && FactoryBean.class.isAssignableFrom(beanClass)) {
			if (!BeanFactoryUtils.isFactoryDereference(name)) {
				// 如果当前 Bean 是 FactoryBean 实例，则返回该 FactoryBean 创建的 bean 的类型信息
				return getTypeForFactoryBean(beanName, mbd, allowFactoryBeanInit).resolve();
			} else {
				// 否则，返回当前 Bean 的 Class 对象
				return beanClass;
			}
		} else {
			// 如果当前 Bean 不是 FactoryBean 实例，则直接返回当前 Bean 的类型信息
			return (!BeanFactoryUtils.isFactoryDereference(name) ? beanClass : null);
		}
	}

	// 根据 beanName 获取与其对应的所有 alias 值
	@Override
	public String[] getAliases(String name) {
		// 获取规范化后的 beanName
		String beanName = transformedBeanName(name);
		List<String> aliases = new ArrayList<>();

		// 判断name前缀是否是 "&"、是否含有 FactoryBean 前缀
		boolean factoryPrefix = name.startsWith(FACTORY_BEAN_PREFIX);
		String fullBeanName = beanName;
		if (factoryPrefix) {
			fullBeanName = FACTORY_BEAN_PREFIX + beanName;
		}

		// 如果 name 对应的 beanName 不等于 fullBeanName 则添加到 alias 列表中
		// （fullBeanName 为添加了 FACTORY_BEAN_PREFIX 前缀的 beanName）
		if (!fullBeanName.equals(name)) {
			aliases.add(fullBeanName);
		}

		// 获取当前 beanName 所有的 alias 值
		String[] retrievedAliases = super.getAliases(beanName);
		String prefix = factoryPrefix ? FACTORY_BEAN_PREFIX : "";
		for (String retrievedAlias : retrievedAliases) {
			String alias = prefix + retrievedAlias;
			if (!alias.equals(name)) {
				aliases.add(alias);
			}
		}

		// 如果当前 BeanFactory 中没有该 Bean 的实例并且它也没有注册过 BeanDefinition，则尝试查找其父 BeanFactory 中的 alias
		if (!containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
			BeanFactory parentBeanFactory = getParentBeanFactory();
			if (parentBeanFactory != null) {
				aliases.addAll(Arrays.asList(parentBeanFactory.getAliases(fullBeanName)));
			}
		}

		// 将 alias 列表转换为 String 数组返回
		return StringUtils.toStringArray(aliases);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	/**
	 * 返回 bean 名称，如果需要，去除工厂引用前缀，并将别名解析为规范名称。
	 *
	 * @param name 用户指定的名称
	 * @return 转换后的 bean 名称
	 */
	protected String transformedBeanName(String name) {
		return canonicalName(BeanFactoryUtils.transformedBeanName(name));
	}

	/**
	 * 检查此 bean 工厂是否包含具有给定名称的 bean 定义。
	 * 不考虑此工厂可能参与的任何层次结构。
	 * 在 {@code containsBean} 未找到缓存的单例实例时调用。
	 * <p>根据具体的 bean 工厂实现的性质，此操作可能很昂贵（例如，由于在外部注册表中进行目录查找）。
	 * 但是，对于可列出的 bean 工厂，这通常只是一个本地哈希查找：因此，该操作是公共接口的一部分。
	 * 在这种情况下，相同的实现可以同时用于此模板方法和公共接口方法。
	 *
	 * @param beanName 要查找的 bean 的名称
	 * @return 如果此 bean 工厂包含具有给定名称的 bean 定义
	 * @see #containsBean
	 * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition
	 */
	protected abstract boolean containsBeanDefinition(String beanName);


	//---------------------------------------------------------------------
	// ConfigurableBeanFactory 接口的实现
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public BeanFactory getParentBeanFactory() {
		return this.parentBeanFactory;
	}

	@Override
	public void setParentBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
			throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
		}
		if (this == parentBeanFactory) {
			throw new IllegalStateException("Cannot set parent bean factory to self");
		}
		this.parentBeanFactory = parentBeanFactory;
	}

	@Override
	public boolean containsLocalBean(String name) {
		String beanName = transformedBeanName(name);
		return ((containsSingleton(beanName) || containsBeanDefinition(beanName)) &&
				(!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName)));
	}

	@Override
	@Nullable
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	@Override
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = (beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader());
	}

	@Override
	@Nullable
	public ClassLoader getTempClassLoader() {
		return this.tempClassLoader;
	}

	@Override
	public void setTempClassLoader(@Nullable ClassLoader tempClassLoader) {
		this.tempClassLoader = tempClassLoader;
	}

	@Override
	public boolean isCacheBeanMetadata() {
		return this.cacheBeanMetadata;
	}

	@Override
	public void setCacheBeanMetadata(boolean cacheBeanMetadata) {
		this.cacheBeanMetadata = cacheBeanMetadata;
	}

	@Override
	@Nullable
	public BeanExpressionResolver getBeanExpressionResolver() {
		return this.beanExpressionResolver;
	}

	@Override
	public void setBeanExpressionResolver(@Nullable BeanExpressionResolver resolver) {
		this.beanExpressionResolver = resolver;
	}

	@Override
	public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar) {
		Assert.notNull(registrar, "PropertyEditorRegistrar must not be null");
		this.propertyEditorRegistrars.add(registrar);
	}

	/**
	 * Return the set of PropertyEditorRegistrars.
	 */
	public Set<PropertyEditorRegistrar> getPropertyEditorRegistrars() {
		return this.propertyEditorRegistrars;
	}

	@Override
	public void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass) {
		Assert.notNull(requiredType, "Required type must not be null");
		Assert.notNull(propertyEditorClass, "PropertyEditor class must not be null");
		this.customEditors.put(requiredType, propertyEditorClass);
	}

	@Override
	public void copyRegisteredEditorsTo(PropertyEditorRegistry registry) {
		registerCustomEditors(registry);
	}

	/**
	 * 将已经在此BeanFactory中注册的自定义编辑器初始化到给定的PropertyEditorRegistry中。
	 * 要调用用于创建和填充bean实例的BeanWrapper，以及用于构造函数参数和工厂方法类型转换的SimpleTypeConverter。
	 *
	 * @param registry 要初始化的PropertyEditorRegistry
	 */
	protected void registerCustomEditors(PropertyEditorRegistry registry) {
		// 如果注册表是PropertyEditorRegistrySupport类型，则使用配置值编辑器。
		if (registry instanceof PropertyEditorRegistrySupport) {
			((PropertyEditorRegistrySupport) registry).useConfigValueEditors();
		}
		// 如果存在PropertyEditorRegistrar，则遍历其并调用其自定义编辑器注册方法。
		if (!this.propertyEditorRegistrars.isEmpty()) {
			for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
				try {
					registrar.registerCustomEditors(registry);
				} catch (BeanCreationException ex) {
					// 如果异常根本原因是BeanCurrentlyInCreationException，则忽略该异常并继续执行注册。
					Throwable rootCause = ex.getMostSpecificCause();
					if (rootCause instanceof BeanCurrentlyInCreationException) {
						BeanCreationException bce = (BeanCreationException) rootCause;
						String bceBeanName = bce.getBeanName();
						if (bceBeanName != null && isCurrentlyInCreation(bceBeanName)) {
							if (logger.isDebugEnabled()) {
								logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName()
										+ "] failed because it tried to obtain currently created bean '"
										+ ex.getBeanName() + "': " + ex.getMessage());
							}
							onSuppressedException(ex);
							continue;
						}
					}
					throw ex;
				}
			}
		}
		// 循环遍历保存在customEditors中的键值对，其中键是要设置编辑器的类型，值是编辑器类。
		// 然后实例化给定类中的新对象并注册到给定的registry中。
		if (!this.customEditors.isEmpty()) {
			this.customEditors.forEach((requiredType, editorClass) ->
					registry.registerCustomEditor(requiredType, BeanUtils.instantiateClass(editorClass)));
		}
	}

	/**
	 * Return the map of custom editors, with Classes as keys and PropertyEditor classes as values.
	 */
	public Map<Class<?>, Class<? extends PropertyEditor>> getCustomEditors() {
		return this.customEditors;
	}

	/**
	 * Return the custom TypeConverter to use, if any.
	 *
	 * @return the custom TypeConverter, or {@code null} if none specified
	 */
	@Nullable
	protected TypeConverter getCustomTypeConverter() {
		return this.typeConverter;
	}

	@Override
	public TypeConverter getTypeConverter() {
		TypeConverter customConverter = getCustomTypeConverter();
		if (customConverter != null) {
			return customConverter;
		} else {
			// Build default TypeConverter, registering custom editors.
			SimpleTypeConverter typeConverter = new SimpleTypeConverter();
			typeConverter.setConversionService(getConversionService());
			registerCustomEditors(typeConverter);
			return typeConverter;
		}
	}

	@Override
	public void setTypeConverter(TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
	}

	@Override
	public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		this.embeddedValueResolvers.add(valueResolver);
	}

	@Override
	public boolean hasEmbeddedValueResolver() {
		return !this.embeddedValueResolvers.isEmpty();
	}

	@Override
	@Nullable
	public String resolveEmbeddedValue(@Nullable String value) {
		if (value == null) {
			return null;
		}
		String result = value;
		for (StringValueResolver resolver : this.embeddedValueResolvers) {
			result = resolver.resolveStringValue(result);
			if (result == null) {
				return null;
			}
		}
		return result;
	}

	@Override
	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
		// Remove from old position, if any
		this.beanPostProcessors.remove(beanPostProcessor);
		// Add to end of list
		this.beanPostProcessors.add(beanPostProcessor);
	}

	/**
	 * Add new BeanPostProcessors that will get applied to beans created
	 * by this factory. To be invoked during factory configuration.
	 *
	 * @see #addBeanPostProcessor
	 * @since 5.3
	 */
	public void addBeanPostProcessors(Collection<? extends BeanPostProcessor> beanPostProcessors) {
		this.beanPostProcessors.removeAll(beanPostProcessors);
		this.beanPostProcessors.addAll(beanPostProcessors);
	}

	@Override
	public int getBeanPostProcessorCount() {
		return this.beanPostProcessors.size();
	}

	/**
	 * Return the list of BeanPostProcessors that will get applied
	 * to beans created with this factory.
	 */
	public List<BeanPostProcessor> getBeanPostProcessors() {
		return this.beanPostProcessors;
	}

	/**
	 * Return whether this factory holds a InstantiationAwareBeanPostProcessor
	 * that will get applied to singleton beans on creation.
	 *
	 * @see #addBeanPostProcessor
	 * @see org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor
	 */
	protected boolean hasInstantiationAwareBeanPostProcessors() {
		return !getBeanPostProcessorCache().instantiationAware.isEmpty();
	}

	/**
	 * Return the internal cache of pre-filtered post-processors,
	 * freshly (re-)building it if necessary.
	 *
	 * @since 5.3
	 */
	BeanPostProcessorCache getBeanPostProcessorCache() {
		BeanPostProcessorCache bpCache = this.beanPostProcessorCache;
		if (bpCache == null) {
			bpCache = new BeanPostProcessorCache();
			for (BeanPostProcessor bp : this.beanPostProcessors) {
				if (bp instanceof InstantiationAwareBeanPostProcessor) {
					bpCache.instantiationAware.add((InstantiationAwareBeanPostProcessor) bp);
					if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
						bpCache.smartInstantiationAware.add((SmartInstantiationAwareBeanPostProcessor) bp);
					}
				}
				if (bp instanceof DestructionAwareBeanPostProcessor) {
					bpCache.destructionAware.add((DestructionAwareBeanPostProcessor) bp);
				}
				if (bp instanceof MergedBeanDefinitionPostProcessor) {
					bpCache.mergedDefinition.add((MergedBeanDefinitionPostProcessor) bp);
				}
			}
			this.beanPostProcessorCache = bpCache;
		}
		return bpCache;
	}

	@Override
	public void registerScope(String scopeName, Scope scope) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		Assert.notNull(scope, "Scope must not be null");
		if (SCOPE_SINGLETON.equals(scopeName) || SCOPE_PROTOTYPE.equals(scopeName)) {
			throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
		}
		Scope previous = this.scopes.put(scopeName, scope);
		if (previous != null && previous != scope) {
			if (logger.isDebugEnabled()) {
				logger.debug("Replacing scope '" + scopeName + "' from [" + previous + "] to [" + scope + "]");
			}
		} else {
			if (logger.isTraceEnabled()) {
				logger.trace("Registering scope '" + scopeName + "' with implementation [" + scope + "]");
			}
		}
	}

	@Override
	public String[] getRegisteredScopeNames() {
		return StringUtils.toStringArray(this.scopes.keySet());
	}

	@Override
	@Nullable
	public Scope getRegisteredScope(String scopeName) {
		Assert.notNull(scopeName, "Scope identifier must not be null");
		return this.scopes.get(scopeName);
	}

	/**
	 * Set the security context provider for this bean factory. If a security manager
	 * is set, interaction with the user code will be executed using the privileged
	 * of the provided security context.
	 */
	public void setSecurityContextProvider(SecurityContextProvider securityProvider) {
		this.securityContextProvider = securityProvider;
	}

	@Override
	public ApplicationStartup getApplicationStartup() {
		return this.applicationStartup;
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		Assert.notNull(applicationStartup, "applicationStartup should not be null");
		this.applicationStartup = applicationStartup;
	}

	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		Assert.notNull(otherFactory, "BeanFactory must not be null");
		setBeanClassLoader(otherFactory.getBeanClassLoader());
		setCacheBeanMetadata(otherFactory.isCacheBeanMetadata());
		setBeanExpressionResolver(otherFactory.getBeanExpressionResolver());
		setConversionService(otherFactory.getConversionService());
		if (otherFactory instanceof AbstractBeanFactory) {
			AbstractBeanFactory otherAbstractFactory = (AbstractBeanFactory) otherFactory;
			this.propertyEditorRegistrars.addAll(otherAbstractFactory.propertyEditorRegistrars);
			this.customEditors.putAll(otherAbstractFactory.customEditors);
			this.typeConverter = otherAbstractFactory.typeConverter;
			this.beanPostProcessors.addAll(otherAbstractFactory.beanPostProcessors);
			this.scopes.putAll(otherAbstractFactory.scopes);
			this.securityContextProvider = otherAbstractFactory.securityContextProvider;
		} else {
			setTypeConverter(otherFactory.getTypeConverter());
			String[] otherScopeNames = otherFactory.getRegisteredScopeNames();
			for (String scopeName : otherScopeNames) {
				this.scopes.put(scopeName, otherFactory.getRegisteredScope(scopeName));
			}
		}
	}

	/**
	 * 返回给定bean名称的“合并”BeanDefinition，
	 * 如果需要，将子bean定义与其父bean合并。
	 * <p>此{@code getMergedBeanDefinition}还考虑祖先中的bean定义。
	 *
	 * @param name 要检索合并定义的bean的名称（可以是别名）
	 * @return 给定bean的（可能合并的）RootBeanDefinition
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @throws BeanDefinitionStoreException  如果bean定义无效
	 */
	@Override
	public BeanDefinition getMergedBeanDefinition(String name) throws BeansException {
		// 根据传入的name，获取转换后的Bean名称
		String beanName = transformedBeanName(name);
		// 高效地检查该工厂是否包含bean的定义
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			// 如果该工厂中不包含bean名称为beanName的bean，且父工厂是可配置Bean工厂，则返回父工厂中beanName的合并Bean定义
			return ((ConfigurableBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(beanName);
		}
		// 否则，本地解析合并的Bean定义
		return getMergedLocalBeanDefinition(beanName);
	}

	/**
	 * 判断给定的bean名称是否是一个FactoryBean。
	 *
	 * @param name 要检查的bean名称
	 * @return 如果给定的bean名称是一个FactoryBean，则返回true；否则返回false
	 * @throws NoSuchBeanDefinitionException 如果找不到bean定义
	 */
	@Override
	public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
		// 转换bean名称
		String beanName = transformedBeanName(name);
		// 获取单例bean实例
		Object beanInstance = getSingleton(beanName, false);
		// 如果bean实例不为空
		if (beanInstance != null) {
			// 判断bean实例是否是FactoryBean的实例
			return (beanInstance instanceof FactoryBean);
		}
		// 没有找到单例实例 -> 检查bean定义
		if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
			// 在当前工厂中没有找到bean定义 -> 委托给父工厂
			return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(name);
		}
		// 判断给定的bean名称对应的合并的本地bean定义是否是FactoryBean
		return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
	}


	/**
	 * 判断指定的bean是否正在创建中。
	 *
	 * @param beanName 要判断的bean的名称
	 * @return 如果指定的bean是单例并且正在创建中，或者是原型并且正在创建中，则返回true；否则返回false。
	 */
	@Override
	public boolean isActuallyInCreation(String beanName) {
		return (isSingletonCurrentlyInCreation(beanName) || isPrototypeCurrentlyInCreation(beanName));
	}

	/**
	 * 返回指定的原型bean是否当前正在创建中（在当前线程内）。
	 *
	 * @param beanName bean的名称
	 * @return 如果指定的原型bean当前正在创建中，则返回true；否则返回false。
	 */
	protected boolean isPrototypeCurrentlyInCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		return (curVal != null &&
				(curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));
	}

	/**
	 * 在原型创建之前的回调方法。
	 * <p>默认实现将原型注册为当前正在创建中。
	 *
	 * @param beanName 即将创建的原型的名称
	 * @see #isPrototypeCurrentlyInCreation
	 */
	@SuppressWarnings("unchecked")
	protected void beforePrototypeCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal == null) {
			this.prototypesCurrentlyInCreation.set(beanName);
		} else if (curVal instanceof String) {
			Set<String> beanNameSet = new HashSet<>(2);
			beanNameSet.add((String) curVal);
			beanNameSet.add(beanName);
			this.prototypesCurrentlyInCreation.set(beanNameSet);
		} else {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.add(beanName);
		}
	}

	/**
	 * 在原型创建之后的回调方法。
	 * <p>默认实现将原型标记为不再创建中。
	 *
	 * @param beanName 已创建的原型的名称
	 * @see #isPrototypeCurrentlyInCreation
	 */
	@SuppressWarnings("unchecked")
	protected void afterPrototypeCreation(String beanName) {
		Object curVal = this.prototypesCurrentlyInCreation.get();
		if (curVal instanceof String) {
			this.prototypesCurrentlyInCreation.remove();
		} else if (curVal instanceof Set) {
			Set<String> beanNameSet = (Set<String>) curVal;
			beanNameSet.remove(beanName);
			if (beanNameSet.isEmpty()) {
				this.prototypesCurrentlyInCreation.remove();
			}
		}
	}

	@Override
	public void destroyBean(String beanName, Object beanInstance) {
		destroyBean(beanName, beanInstance, getMergedLocalBeanDefinition(beanName));
	}

	/**
	 * 根据给定的bean定义，销毁给定的bean实例（通常是从该工厂获取的原型实例）。
	 *
	 * @param beanName bean定义的名称
	 * @param bean     要销毁的bean实例
	 * @param mbd      合并的bean定义
	 */
	protected void destroyBean(String beanName, Object bean, RootBeanDefinition mbd) {
		new DisposableBeanAdapter(
				bean, beanName, mbd, getBeanPostProcessorCache().destructionAware, getAccessControlContext()).destroy();
	}

	@Override
	public void destroyScopedBean(String beanName) {
		RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		if (mbd.isSingleton() || mbd.isPrototype()) {
			throw new IllegalArgumentException(
					"Bean name '" + beanName + "' does not correspond to an object in a mutable scope");
		}
		String scopeName = mbd.getScope();
		Scope scope = this.scopes.get(scopeName);
		if (scope == null) {
			throw new IllegalStateException("No Scope SPI registered for scope name '" + scopeName + "'");
		}
		Object bean = scope.remove(beanName);
		if (bean != null) {
			destroyBean(beanName, bean, mbd);
		}
	}

	/**
	 * 确定原始的bean名称，将本地定义的别名解析为规范名称。
	 *
	 * @param name 用户指定的名称
	 * @return 原始的bean名称
	 */
	protected String originalBeanName(String name) {
		String beanName = transformedBeanName(name);
		if (name.startsWith(FACTORY_BEAN_PREFIX)) {
			beanName = FACTORY_BEAN_PREFIX + beanName;
		}
		return beanName;
	}


	//---------------------------------------------------------------------
	// Implementation methods
	//---------------------------------------------------------------------

	/**
	 * 使用此工厂注册的自定义编辑器初始化给定的BeanWrapper。
	 * 用于将创建和填充bean实例的BeanWrapper调用。
	 * <p>默认实现委托给{@link #registerCustomEditors}。
	 * 可以在子类中重写。
	 *
	 * @param bw 要初始化的BeanWrapper
	 */
	protected void initBeanWrapper(BeanWrapper bw) {
		bw.setConversionService(getConversionService());
		registerCustomEditors(bw);
	}

	@Override
	@Nullable
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	@Override
	public void setConversionService(@Nullable ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * 返回一个合并的RootBeanDefinition，如果指定的bean对应于子bean定义，则遍历父bean定义。
	 *
	 * @param beanName 要检索合并定义的bean的名称
	 * @return 给定bean的（可能合并的）RootBeanDefinition
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的bean
	 * @throws BeanDefinitionStoreException  如果bean定义无效
	 */
	protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
		// 首先快速检查并发映射，锁定最少。
		RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
		if (mbd != null && !mbd.stale) {
			return mbd;
		}
		return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
	}

	/**
	 * 返回给定顶级bean的RootBeanDefinition，通过与父级合并，如果给定bean的定义是子bean定义的话。
	 *
	 * @param beanName bean定义的名称
	 * @param bd       原始bean定义（Root/ChildBeanDefinition）
	 * @return 给定bean的（可能合并的）RootBeanDefinition
	 * @throws BeanDefinitionStoreException 如果bean定义无效
	 */
	protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd)
			throws BeanDefinitionStoreException {

		return getMergedBeanDefinition(beanName, bd, null);
	}

	/**
	 * 获取合并后的 BeanDefinition。
	 *
	 * @param beanName     Bean 的名称
	 * @param bd           当前 BeanDefinition 实例
	 * @param containingBd 包含当前 BeanDefinition 的父级 BeanDefinition，这是一种特殊的 BeanDefinition才会用上，innerBean，在某个 XML Bean 属性中再定义Bean的时候
	 * @return 合并后的 BeanDefinition 实例
	 * @throws BeanDefinitionStoreException 如果无法获取 BeanDefinition，则会抛出 BeanDefinitionStoreException 异常
	 */
	protected RootBeanDefinition getMergedBeanDefinition(
			String beanName, BeanDefinition bd, @Nullable BeanDefinition containingBd)
			throws BeanDefinitionStoreException {

		// 同步访问 mergedBeanDefinitions Map
		synchronized (this.mergedBeanDefinitions) {
			RootBeanDefinition mbd = null; // 定义 RootBeanDefinition 对象，用于存储合并后的 BeanDefinition 实例
			RootBeanDefinition previous = null; // 定义 RootBeanDefinition 对象，用于存储之前被合并的 BeanDefinition

			// 如果当前的 BeanDefinition 不包含在任何父级 BeanDefinition 中，则从 mergedBeanDefinitions Map 中获取合并后的 BeanDefinition
			if (containingBd == null) {
				mbd = this.mergedBeanDefinitions.get(beanName);
			}

			// 如果当前的 BeanDefinition 没有被缓存或者已经变得过时，则进行合并操作
			if (mbd == null || mbd.stale) {
				previous = mbd; // 存储之前的 BeanDefinition

				// 如果当前 BeanDefinition 没有父级，则直接获取实例的一个副本进行合并
				if (bd.getParentName() == null) {
					if (bd instanceof RootBeanDefinition) { // 如果当前 BeanDefinition 是 RootBeanDefinition 类型，则直接复制
						mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();
					} else { // 否则创建一个新的 RootBeanDefinition 对象，并拷贝传入的 BeanDefinition 的属性
						mbd = new RootBeanDefinition(bd);
					}
				} else { // 如果当前 BeanDefinition 有父级，则需要将父级 BeanDefinition 和当前 BeanDefinition 进行合并
					BeanDefinition pbd;
					try {
						String parentBeanName = transformedBeanName(bd.getParentName()); // 获取父级 BeanDefinition 的名字

						// 判断父级与当前 Bean 是否相同，如果不同，则从容器中获取父级 BeanDefinition，否则从父级的 ConfigurableBeanFactory 获取 BeanDefinition
						if (!beanName.equals(parentBeanName)) {
							pbd = getMergedBeanDefinition(parentBeanName);
						} else {
							BeanFactory parent = getParentBeanFactory();
							if (parent instanceof ConfigurableBeanFactory) {
								pbd = ((ConfigurableBeanFactory) parent).getMergedBeanDefinition(parentBeanName);
							} else { // 如果父级不是 ConfigurableBeanFactory 类型则抛出 NoSuchBeanDefinitionException 异常
								throw new NoSuchBeanDefinitionException(parentBeanName,
										"Parent name '" + parentBeanName + "' is equal to bean name '" + beanName +
												"': cannot be resolved without a ConfigurableBeanFactory parent");
							}
						}
					} catch (
							NoSuchBeanDefinitionException ex) { // 如果无法获取父级的 BeanDefinition，则抛出 BeanDefinitionStoreException 异常
						throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
								"Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
					}

					// 将父级 BeanDefinition 和当前 BeanDefinition 进行深度复制，当前 BeanDefinition 中的属性会覆盖父级 BeanDefinition 中的属性
					mbd = new RootBeanDefinition(pbd);
					mbd.overrideFrom(bd);
				}

				// 如果当前 BeanDefinition 没有被设置 Scope，那么默认设置为 Singleton
				if (!StringUtils.hasLength(mbd.getScope())) {
					mbd.setScope(SCOPE_SINGLETON);
				}

				// 如果当前 BeanDefinition 包含在非 Singleton Bean 中，那么该 Bean 也不是 Singleton，需要从父级 BeanDefinition 中继承 Scope 属性
				if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
					mbd.setScope(containingBd.getScope());
				}

				// 缓存合并的 BeanDefinition
				if (containingBd == null && isCacheBeanMetadata()) {
					this.mergedBeanDefinitions.put(beanName, mbd);
				}
			}
			if (previous != null) {
				copyRelevantMergedBeanDefinitionCaches(previous, mbd);
			}
			return mbd;
		}
	}

	/**
	 * 复制相关的合并Bean定义缓存。
	 * 如果给定的RootBeanDefinition与先前的RootBeanDefinition具有相同的类名、工厂Bean名称和工厂方法名称，则进行复制。
	 * 如果目标类型与先前的目标类型相同，则进行复制。
	 *
	 * @param previous 先前的RootBeanDefinition
	 * @param mbd      当前的RootBeanDefinition
	 */
	private void copyRelevantMergedBeanDefinitionCaches(RootBeanDefinition previous, RootBeanDefinition mbd) {
		if (ObjectUtils.nullSafeEquals(mbd.getBeanClassName(), previous.getBeanClassName()) &&
				ObjectUtils.nullSafeEquals(mbd.getFactoryBeanName(), previous.getFactoryBeanName()) &&
				ObjectUtils.nullSafeEquals(mbd.getFactoryMethodName(), previous.getFactoryMethodName())) {
			ResolvableType targetType = mbd.targetType;
			ResolvableType previousTargetType = previous.targetType;
			if (targetType == null || targetType.equals(previousTargetType)) {
				mbd.targetType = previousTargetType;
				mbd.isFactoryBean = previous.isFactoryBean;
				mbd.resolvedTargetType = previous.resolvedTargetType;
				mbd.factoryMethodReturnType = previous.factoryMethodReturnType;
				mbd.factoryMethodToIntrospect = previous.factoryMethodToIntrospect;
			}
		}
	}

	/**
	 * Check the given merged bean definition,
	 * potentially throwing validation exceptions.
	 *
	 * @param mbd      the merged bean definition to check
	 * @param beanName the name of the bean
	 * @param args     the arguments for bean creation, if any
	 * @throws BeanDefinitionStoreException in case of validation failure
	 */
	protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, @Nullable Object[] args)
			throws BeanDefinitionStoreException {

		if (mbd.isAbstract()) {
			throw new BeanIsAbstractException(beanName);
		}
	}

	/**
	 * Remove the merged bean definition for the specified bean,
	 * recreating it on next access.
	 *
	 * @param beanName the bean name to clear the merged definition for
	 */
	protected void clearMergedBeanDefinition(String beanName) {
		RootBeanDefinition bd = this.mergedBeanDefinitions.get(beanName);
		if (bd != null) {
			bd.stale = true;
		}
	}

	/**
	 * Clear the merged bean definition cache, removing entries for beans
	 * which are not considered eligible for full metadata caching yet.
	 * <p>Typically triggered after changes to the original bean definitions,
	 * e.g. after applying a {@code BeanFactoryPostProcessor}. Note that metadata
	 * for beans which have already been created at this point will be kept around.
	 *
	 * @since 4.2
	 */
	public void clearMetadataCache() {
		this.mergedBeanDefinitions.forEach((beanName, bd) -> {
			if (!isBeanEligibleForMetadataCaching(beanName)) {
				bd.stale = true;
			}
		});
	}

	/**
	 * Determine whether the specified bean is eligible for having
	 * its bean definition metadata cached.
	 *
	 * @param beanName the name of the bean
	 * @return {@code true} if the bean's metadata may be cached
	 * at this point already
	 */
	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
		return this.alreadyCreated.contains(beanName);
	}

	/**
	 * 解析指定bean定义的bean类，
	 * 将bean类名解析为Class引用（如果需要），
	 * 并将解析后的Class存储在bean定义中以供进一步使用。
	 *
	 * @param mbd          要确定类的合并的bean定义
	 * @param beanName     bean的名称（用于错误处理）
	 * @param typesToMatch 用于内部类型匹配的匹配类型
	 *                     （还表示返回的Class将永远不会暴露给应用程序代码）
	 * @return 解析后的bean类（如果没有则为null）
	 * @throws CannotLoadBeanClassException 如果无法加载类
	 */
	@Nullable
	protected Class<?> resolveBeanClass(RootBeanDefinition mbd, String beanName, Class<?>... typesToMatch)
			throws CannotLoadBeanClassException {
		try {
			// 如果bean定义中已经有bean类，则直接返回
			if (mbd.hasBeanClass()) {
				return mbd.getBeanClass();
			}
			// 如果有安全管理器，则通过特权访问方式解析bean类
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged((PrivilegedExceptionAction<Class<?>>)
						() -> doResolveBeanClass(mbd, typesToMatch), getAccessControlContext());
			} else {
				// 否则直接解析bean类
				return doResolveBeanClass(mbd, typesToMatch);
			}
		} catch (PrivilegedActionException pae) {
			// 如果解析过程中发生异常，则抛出CannotLoadBeanClassException异常
			ClassNotFoundException ex = (ClassNotFoundException) pae.getException();
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		} catch (ClassNotFoundException ex) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
		} catch (LinkageError err) {
			throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), err);
		}
	}


	@Nullable
	private Class<?> doResolveBeanClass(RootBeanDefinition mbd, Class<?>... typesToMatch)
			throws ClassNotFoundException {

		// 获取bean的ClassLoader
		ClassLoader beanClassLoader = getBeanClassLoader();
		// 保存动态加载类的ClassLoader
		ClassLoader dynamicLoader = beanClassLoader;
		// 判断是否需要重新加载
		boolean freshResolve = false;

		// 如果存在类型匹配，则使用临时ClassLoader来进行类型检查（而不创建实例）。
		// 用于方法织入等特定场景下。
		if (!ObjectUtils.isEmpty(typesToMatch)) {
			ClassLoader tempClassLoader = getTempClassLoader();
			if (tempClassLoader != null) {
				// 如果存在临时ClassLoader，将动态加载类的ClassLoader指向临时ClassLoader
				dynamicLoader = tempClassLoader;
				// 将freshResolve设置为true，表示需要重新加载
				freshResolve = true;
				// 如果临时ClassLoader是DecoratorClassLoader，则排除所有typesToMatch
				if (tempClassLoader instanceof DecoratingClassLoader) {
					DecoratingClassLoader dcl = (DecoratingClassLoader) tempClassLoader;
					for (Class<?> typeToMatch : typesToMatch) {
						dcl.excludeClass(typeToMatch.getName());
					}
				}
			}
		}

		// 获取bean的全类名
		String className = mbd.getBeanClassName();
		if (className != null) {
			// 对全类名进行表达式求值
			Object evaluated = evaluateBeanDefinitionString(className, mbd);
			if (!className.equals(evaluated)) {
				// 如果表达式求值得到Class，则返回该Class
				if (evaluated instanceof Class) {
					return (Class<?>) evaluated;
					// 如果表达式求值得到String，则重新赋值className
				} else if (evaluated instanceof String) {
					className = (String) evaluated;
					// 将freshResolve设置为true，表示需要重新加载
					freshResolve = true;
				} else {
					throw new IllegalStateException("Invalid class name expression result: " + evaluated);
				}
			}
			// 如果freshResolve为true，则尝试使用动态ClassLoader来进行类加载。
			// 避免将加载到的类对象存储在BeanDefinition中
			if (freshResolve && dynamicLoader != null) {
				try {
					return dynamicLoader.loadClass(className);
				} catch (ClassNotFoundException ex) {
					// 如果找不到类，则记录日志
					if (logger.isTraceEnabled()) {
						logger.trace("Could not load class [" + className + "] from " + dynamicLoader + ": " + ex);
					}
				}
			}
			// 否则使用常规方式进行类加载
			return ClassUtils.forName(className, dynamicLoader);
		}

		// 如果无法获取全类名，则设置使用bean的ClassLoader进行类加载，并在BeanDefinition中缓存结果
		return mbd.resolveBeanClass(beanClassLoader);
	}


	/**
	 * Evaluate the given String as contained in a bean definition,
	 * potentially resolving it as an expression.
	 *
	 * @param value          the value to check
	 * @param beanDefinition the bean definition that the value comes from
	 * @return the resolved value
	 * @see #setBeanExpressionResolver
	 */
	@Nullable
	protected Object evaluateBeanDefinitionString(@Nullable String value, @Nullable BeanDefinition beanDefinition) {
		if (this.beanExpressionResolver == null) {
			return value;
		}

		Scope scope = null;
		if (beanDefinition != null) {
			String scopeName = beanDefinition.getScope();
			if (scopeName != null) {
				scope = getRegisteredScope(scopeName);
			}
		}
		return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
	}


	/**
	 * Predict the eventual bean type (of the processed bean instance) for the
	 * specified bean. Called by {@link #getType} and {@link #isTypeMatch}.
	 * Does not need to handle FactoryBeans specifically, since it is only
	 * supposed to operate on the raw bean type.
	 * <p>This implementation is simplistic in that it is not able to
	 * handle factory methods and InstantiationAwareBeanPostProcessors.
	 * It only predicts the bean type correctly for a standard bean.
	 * To be overridden in subclasses, applying more sophisticated type detection.
	 *
	 * @param beanName     the name of the bean
	 * @param mbd          the merged bean definition to determine the type for
	 * @param typesToMatch the types to match in case of internal type matching purposes
	 *                     (also signals that the returned {@code Class} will never be exposed to application code)
	 * @return the type of the bean, or {@code null} if not predictable
	 */
	@Nullable
	protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		Class<?> targetType = mbd.getTargetType();
		if (targetType != null) {
			return targetType;
		}
		if (mbd.getFactoryMethodName() != null) {
			return null;
		}
		return resolveBeanClass(mbd, beanName, typesToMatch);
	}

	/**
	 * Check whether the given bean is defined as a {@link FactoryBean}.
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the corresponding bean definition
	 */
	protected boolean isFactoryBean(String beanName, RootBeanDefinition mbd) {
		Boolean result = mbd.isFactoryBean;
		if (result == null) {
			Class<?> beanType = predictBeanType(beanName, mbd, FactoryBean.class);
			result = (beanType != null && FactoryBean.class.isAssignableFrom(beanType));
			mbd.isFactoryBean = result;
		}
		return result;
	}

	/**
	 * 尽可能确定给定FactoryBean定义的bean类型。
	 * 仅在目标bean尚未注册为单例实例时调用。
	 * 如果{@code allowInit}为{@code true}且无法以其他方式确定类型，则实现允许实例化目标工厂bean；
	 * 否则，它仅限于内省签名和相关元数据。
	 * <p>如果在bean定义上未设置{@link FactoryBean#OBJECT_TYPE_ATTRIBUTE}并且{@code allowInit}为{@code true}，
	 * 则默认实现将通过{@code getBean}创建FactoryBean来调用其{@code getObjectType}方法。
	 * 鼓励子类进行优化，通常是通过检查工厂bean类的泛型签名或创建它的工厂方法来实现。
	 * 如果子类实例化FactoryBean，则应考虑尝试{@code getObjectType}方法而不完全填充bean。
	 * 如果失败，则应使用此实现执行的完整FactoryBean创建作为回退。
	 *
	 * @param beanName  bean的名称
	 * @param mbd       bean的合并定义
	 * @param allowInit 如果允许初始化FactoryBean，如果无法以其他方式确定类型
	 * @return 如果可以确定bean的类型，则为bean的类型，否则为{@code ResolvableType.NONE}
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 * @see #getBean(String)
	 * @since 5.2
	 */
	protected ResolvableType getTypeForFactoryBean(String beanName, RootBeanDefinition mbd, boolean allowInit) {
		// 从bean定义的属性中获取FactoryBean的类型
		ResolvableType result = getTypeForFactoryBeanFromAttributes(mbd);
		if (result != ResolvableType.NONE) {
			return result;
		}
		if (allowInit && mbd.isSingleton()) {
			try {
				// 通过getBean方法获取FactoryBean实例
				FactoryBean<?> factoryBean = doGetBean(FACTORY_BEAN_PREFIX + beanName, FactoryBean.class, null, true);
				// 获取FactoryBean的类型
				Class<?> objectType = getTypeForFactoryBean(factoryBean);
				return (objectType != null ? ResolvableType.forClass(objectType) : ResolvableType.NONE);
			} catch (BeanCreationException ex) {
				if (ex.contains(BeanCurrentlyInCreationException.class)) {
					logger.trace(LogMessage.format("Bean currently in creation on FactoryBean type check: %s", ex));
				} else if (mbd.isLazyInit()) {
					logger.trace(LogMessage.format("Bean creation exception on lazy FactoryBean type check: %s", ex));
				} else {
					logger.debug(LogMessage.format("Bean creation exception on eager FactoryBean type check: %s", ex));
				}
				onSuppressedException(ex);
			}
		}
		return ResolvableType.NONE;
	}

	/**
	 * Determine the bean type for a FactoryBean by inspecting its attributes for a
	 * {@link FactoryBean#OBJECT_TYPE_ATTRIBUTE} value.
	 *
	 * @param attributes the attributes to inspect
	 * @return a {@link ResolvableType} extracted from the attributes or
	 * {@code ResolvableType.NONE}
	 * @since 5.2
	 */
	ResolvableType getTypeForFactoryBeanFromAttributes(AttributeAccessor attributes) {
		Object attribute = attributes.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
		if (attribute instanceof ResolvableType) {
			return (ResolvableType) attribute;
		}
		if (attribute instanceof Class) {
			return ResolvableType.forClass((Class<?>) attribute);
		}
		return ResolvableType.NONE;
	}

	/**
	 * Determine the bean type for the given FactoryBean definition, as far as possible.
	 * Only called if there is no singleton instance registered for the target bean already.
	 * <p>The default implementation creates the FactoryBean via {@code getBean}
	 * to call its {@code getObjectType} method. Subclasses are encouraged to optimize
	 * this, typically by just instantiating the FactoryBean but not populating it yet,
	 * trying whether its {@code getObjectType} method already returns a type.
	 * If no type found, a full FactoryBean creation as performed by this implementation
	 * should be used as fallback.
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the merged bean definition for the bean
	 * @return the type for the bean if determinable, or {@code null} otherwise
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 * @see #getBean(String)
	 * @deprecated since 5.2 in favor of {@link #getTypeForFactoryBean(String, RootBeanDefinition, boolean)}
	 */
	@Nullable
	@Deprecated
	protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
		return getTypeForFactoryBean(beanName, mbd, true).resolve();
	}

	/**
	 * 将指定的bean标记为已创建（或即将创建）。
	 * <p>这允许bean工厂针对重复创建指定的bean进行缓存优化。
	 *
	 * @param beanName bean的名称
	 */
	protected void markBeanAsCreated(String beanName) {
		if (!this.alreadyCreated.contains(beanName)) {
			synchronized (this.mergedBeanDefinitions) {
				if (!this.alreadyCreated.contains(beanName)) {
					// 现在我们实际上正在创建bean，让bean定义重新合并...
					// 以防在此期间更改了一些元数据。
					clearMergedBeanDefinition(beanName);
					this.alreadyCreated.add(beanName);
				}
			}
		}
	}

	/**
	 * 在bean创建失败后，执行适当的清理缓存的元数据。
	 *
	 * @param beanName bean的名称
	 */
	protected void cleanupAfterBeanCreationFailure(String beanName) {
		synchronized (this.mergedBeanDefinitions) {
			this.alreadyCreated.remove(beanName);
		}
	}

	/**
	 * 删除给定 bean 名称的单例实例（如果有），但前提是该实例除类型检查外尚未用于其他目的。
	 *
	 * @param beanName the name of the bean
	 * @return {@code true} if actually removed, {@code false} otherwise
	 */
	protected boolean removeSingletonIfCreatedForTypeCheckOnly(String beanName) {
		if (!this.alreadyCreated.contains(beanName)) {
			removeSingleton(beanName);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 检查该工厂的 bean 创建阶段是否已经开始，即是否有任何 bean 同时被标记为已创建。
	 *
	 * @see #markBeanAsCreated
	 * @since 4.2.2
	 */
	protected boolean hasBeanCreationStarted() {
		return !this.alreadyCreated.isEmpty();
	}

	/**
	 * 获取给定bean实例的对象，可以是bean实例本身，也可以是FactoryBean创建的对象。
	 *
	 * @param beanInstance 共享的bean实例
	 * @param name         包含工厂解引用前缀的名称
	 * @param beanName     规范的bean名称
	 * @param mbd          合并的bean定义
	 * @return 要暴露给bean的对象
	 */
	protected Object getObjectForBeanInstance(Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

		// 如果bean不是一个工厂，则不要让调用代码尝试进行工厂解引用。
		if (BeanFactoryUtils.isFactoryDereference(name)) {
			if (beanInstance instanceof NullBean) {
				return beanInstance;
			}
			if (!(beanInstance instanceof FactoryBean)) {
				throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
			}
			if (mbd != null) {
				mbd.isFactoryBean = true;
			}
			return beanInstance;
		}

		// 现在我们有了bean实例，它可能是一个普通的bean或者是一个FactoryBean。
		// 如果它是FactoryBean，则会使用它创建一个bean实例，除非调用者实际上想要一个引用工厂。
		if (!(beanInstance instanceof FactoryBean)) {
			return beanInstance;
		}

		Object object = null;
		if (mbd != null) {
			mbd.isFactoryBean = true;
		} else {
			object = getCachedObjectForFactoryBean(beanName);
		}
		if (object == null) {
			// 从工厂中返回bean实例。
			FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
			// 如果是单例的，则从FactoryBean中获取对象并缓存起来。
			if (mbd == null && containsBeanDefinition(beanName)) {
				mbd = getMergedLocalBeanDefinition(beanName);
			}
			boolean synthetic = (mbd != null && mbd.isSynthetic());
			object = getObjectFromFactoryBean(factory, beanName, !synthetic);
		}
		return object;
	}

	/**
	 * 判断给定的bean名称在此工厂中是否已经被使用，
	 * 即是否已经有一个本地bean或别名注册在此名称下，
	 * 或者是否已经创建了一个使用此名称的内部bean。
	 *
	 * @param beanName 要检查的名称
	 */
	public boolean isBeanNameInUse(String beanName) {
		return isAlias(beanName) || containsLocalBean(beanName) || hasDependentBean(beanName);
	}

	/**
	 * 将给定的bean添加到此工厂中的一次性bean列表中，
	 * 注册其DisposableBean接口和/或给定的销毁方法，
	 * 在工厂关闭时调用（如果适用）。仅适用于单例。
	 *
	 * @param beanName bean的名称
	 * @param bean     bean的实例
	 * @param mbd      bean的定义
	 * @see RootBeanDefinition#isSingleton
	 * @see RootBeanDefinition#getDependsOn
	 * @see #registerDisposableBean
	 * @see #registerDependentBean
	 */
	protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
		AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
		if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
			if (mbd.isSingleton()) {
				// 注册一个DisposableBean实现，该实现执行给定bean的所有销毁工作：
				// DestructionAwareBeanPostProcessors、DisposableBean接口、自定义销毁方法。
				registerDisposableBean(beanName, new DisposableBeanAdapter(
						bean, beanName, mbd, getBeanPostProcessorCache().destructionAware, acc));
			} else {
				// 一个具有自定义作用域的bean...
				Scope scope = this.scopes.get(mbd.getScope());
				if (scope == null) {
					throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
				}
				scope.registerDestructionCallback(beanName, new DisposableBeanAdapter(
						bean, beanName, mbd, getBeanPostProcessorCache().destructionAware, acc));
			}
		}
	}

	/**
	 * Delegate the creation of the access control context to the
	 * {@link #setSecurityContextProvider SecurityContextProvider}.
	 */
	@Override
	public AccessControlContext getAccessControlContext() {
		return (this.securityContextProvider != null ?
				this.securityContextProvider.getAccessControlContext() :
				AccessController.getContext());
	}

	/**
	 * Determine whether the given bean requires destruction on shutdown.
	 * <p>The default implementation checks the DisposableBean interface as well as
	 * a specified destroy method and registered DestructionAwareBeanPostProcessors.
	 *
	 * @param bean the bean instance to check
	 * @param mbd  the corresponding bean definition
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see AbstractBeanDefinition#getDestroyMethodName()
	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
	 */
	protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
		return (bean.getClass() != NullBean.class && (DisposableBeanAdapter.hasDestroyMethod(bean, mbd) ||
				(hasDestructionAwareBeanPostProcessors() && DisposableBeanAdapter.hasApplicableProcessors(
						bean, getBeanPostProcessorCache().destructionAware))));
	}


	//---------------------------------------------------------------------
	// Abstract methods to be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * Return whether this factory holds a DestructionAwareBeanPostProcessor
	 * that will get applied to singleton beans on shutdown.
	 *
	 * @see #addBeanPostProcessor
	 * @see org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
	 */
	protected boolean hasDestructionAwareBeanPostProcessors() {
		return !getBeanPostProcessorCache().destructionAware.isEmpty();
	}

	/**
	 * Return the bean definition for the given bean name.
	 * Subclasses should normally implement caching, as this method is invoked
	 * by this class every time bean definition metadata is needed.
	 * <p>Depending on the nature of the concrete bean factory implementation,
	 * this operation might be expensive (for example, because of directory lookups
	 * in external registries). However, for listable bean factories, this usually
	 * just amounts to a local hash lookup: The operation is therefore part of the
	 * public interface there. The same implementation can serve for both this
	 * template method and the public interface method in that case.
	 *
	 * @param beanName the name of the bean to find a definition for
	 * @return the BeanDefinition for this prototype name (never {@code null})
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if the bean definition cannot be resolved
	 * @throws BeansException                                                  in case of errors
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#getBeanDefinition
	 */
	protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

	/**
	 * Create a bean instance for the given merged bean definition (and arguments).
	 * The bean definition will already have been merged with the parent definition
	 * in case of a child definition.
	 * <p>All bean retrieval methods delegate to this method for actual bean creation.
	 *
	 * @param beanName the name of the bean
	 * @param mbd      the merged bean definition for the bean
	 * @param args     explicit arguments to use for constructor or factory method invocation
	 * @return a new instance of the bean
	 * @throws BeanCreationException if the bean could not be created
	 */
	protected abstract Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
			throws BeanCreationException;

	/**
	 * Internal cache of pre-filtered post-processors.
	 *
	 * @since 5.3
	 */
	static class BeanPostProcessorCache {

		final List<InstantiationAwareBeanPostProcessor> instantiationAware = new ArrayList<>();

		final List<SmartInstantiationAwareBeanPostProcessor> smartInstantiationAware = new ArrayList<>();

		final List<DestructionAwareBeanPostProcessor> destructionAware = new ArrayList<>();

		final List<MergedBeanDefinitionPostProcessor> mergedDefinition = new ArrayList<>();
	}

	/**
	 * CopyOnWriteArrayList which resets the beanPostProcessorCache field on modification.
	 *
	 * @since 5.3
	 */
	private class BeanPostProcessorCacheAwareList extends CopyOnWriteArrayList<BeanPostProcessor> {

		@Override
		public BeanPostProcessor set(int index, BeanPostProcessor element) {
			BeanPostProcessor result = super.set(index, element);
			beanPostProcessorCache = null;
			return result;
		}

		@Override
		public boolean add(BeanPostProcessor o) {
			boolean success = super.add(o);
			beanPostProcessorCache = null;
			return success;
		}

		@Override
		public void add(int index, BeanPostProcessor element) {
			super.add(index, element);
			beanPostProcessorCache = null;
		}

		@Override
		public BeanPostProcessor remove(int index) {
			BeanPostProcessor result = super.remove(index);
			beanPostProcessorCache = null;
			return result;
		}

		@Override
		public boolean remove(Object o) {
			boolean success = super.remove(o);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			boolean success = super.removeAll(c);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			boolean success = super.retainAll(c);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean addAll(Collection<? extends BeanPostProcessor> c) {
			boolean success = super.addAll(c);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean addAll(int index, Collection<? extends BeanPostProcessor> c) {
			boolean success = super.addAll(index, c);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public boolean removeIf(Predicate<? super BeanPostProcessor> filter) {
			boolean success = super.removeIf(filter);
			if (success) {
				beanPostProcessorCache = null;
			}
			return success;
		}

		@Override
		public void replaceAll(UnaryOperator<BeanPostProcessor> operator) {
			super.replaceAll(operator);
			beanPostProcessorCache = null;
		}
	}

}
