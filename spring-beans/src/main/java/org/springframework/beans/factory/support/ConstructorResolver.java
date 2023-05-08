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

import org.apache.commons.logging.Log;
import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.CollectionFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.beans.ConstructorProperties;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * Delegate for resolving constructors and factory methods.
 *
 * <p>Performs constructor resolution through argument matching.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @author Costin Leau
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @see #autowireConstructor
 * @see #instantiateUsingFactoryMethod
 * @see AbstractAutowireCapableBeanFactory
 * @since 2.0
 */
class ConstructorResolver {

	private static final Object[] EMPTY_ARGS = new Object[0];

	/**
	 * Marker for autowired arguments in a cached argument array, to be replaced
	 * by a {@linkplain #resolveAutowiredArgument resolved autowired argument}.
	 */
	private static final Object autowiredArgumentMarker = new Object();

	private static final NamedThreadLocal<InjectionPoint> currentInjectionPoint = new NamedThreadLocal<>("Current injection point");


	private final AbstractAutowireCapableBeanFactory beanFactory;

	private final Log logger;


	/**
	 * Create a new ConstructorResolver for the given factory and instantiation strategy.
	 *
	 * @param beanFactory the BeanFactory to work with
	 */
	public ConstructorResolver(AbstractAutowireCapableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.logger = beanFactory.getLogger();
	}

	/**
	 * 设置当前注入点，并返回旧值。
	 * <p>用于实现依赖注入的一个方法。它用于在解析构造函数参数的过程中跟踪当前的注入点（Injection Point），以便能够在必要时解决循环依赖问题并进行合适的依赖注入。
	 * 此方法的作用是设置当前解析的构造函数参数的注入点。
	 * 具体来说，当扫描到构造函数的第 i 个参数时，将调用 setCurrentInjectionPoint(i) 方法，并将参数的索引号 i 作为其参数传递进去。
	 * 这样，当后续需要进行循环依赖处理或者对该参数进行依赖注入时，便可利用该注入点的信息进行相应的处理。</p>
	 *
	 * @param injectionPoint 注入点
	 * @return 旧的注入点
	 */
	static InjectionPoint setCurrentInjectionPoint(@Nullable InjectionPoint injectionPoint) {
		InjectionPoint old = currentInjectionPoint.get();
		if (injectionPoint != null) {
			currentInjectionPoint.set(injectionPoint);
		} else {
			currentInjectionPoint.remove();
		}
		return old;
	}

	/**
	 * 使用构造函数自动注入bean的方式创建BeanWrapper实例。
	 * 当指定了显式的构造函数参数值时，也适用于将所有剩余参数与bean工厂中的bean匹配。
	 * <p>这对应于构造函数注入：在这种模式下，Spring bean工厂能够托管期望基于构造函数的依赖项解析的组件。
	 *
	 * @param beanName     bean的名称
	 * @param mbd          用于bean的合并定义
	 * @param chosenCtors  选定的候选构造函数（如果没有，则为null）
	 * @param explicitArgs 通过getBean方法在程序上通过传递参数值的方式传入的参数值，如果没有，则为null（->使用bean定义中的构造函数参数值）。
	 * @return 新实例的BeanWrapper
	 */
	public BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd, @Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {

		//创建BeanWrapperImpl实例
		BeanWrapperImpl bw = new BeanWrapperImpl();
		this.beanFactory.initBeanWrapper(bw);

		//初始化局部变量
		Constructor<?> constructorToUse = null;
		ArgumentsHolder argsHolderToUse = null;
		Object[] argsToUse = null;

		if (explicitArgs != null) {
			//使用显式提供的参数
			argsToUse = explicitArgs;
		} else {
			Object[] argsToResolve = null;
			synchronized (mbd.constructorArgumentLock) {
				//检查是否有缓存的构造函数和参数值
				constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse != null && mbd.constructorArgumentsResolved) {
					//发现带有缓存的构造函数
					argsToUse = mbd.resolvedConstructorArguments;
					if (argsToUse == null) {
						argsToResolve = mbd.preparedConstructorArguments;
					}
				}
			}
			if (argsToResolve != null) {
				//解析预备参数
				argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve);
			}
		}

		if (constructorToUse == null || argsToUse == null) {
			//使用指定的构造函数（如果有）
			Constructor<?>[] candidates = chosenCtors;
			if (candidates == null) {
				Class<?> beanClass = mbd.getBeanClass();
				try {
					//获取全部公共构造函数或非公共构造函数
					candidates = (mbd.isNonPublicAccessAllowed() ? beanClass.getDeclaredConstructors() : beanClass.getConstructors());
				} catch (Throwable ex) {
					//构造函数解析错误
					throw new BeanCreationException(mbd.getResourceDescription(), beanName, "从类加载器[" + beanClass.getClassLoader() + "]解析bean类[" + beanClass.getName() + "]的声明构造函数失败", ex);
				}
			}

			if (candidates.length == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
				//只有一个构造函数，且没有指定参数值或构造函数参数值，则不需要解析
				Constructor<?> uniqueCandidate = candidates[0];
				if (uniqueCandidate.getParameterCount() == 0) {
					//构造函数没有参数，直接实例化对象
					synchronized (mbd.constructorArgumentLock) {
						mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
						mbd.constructorArgumentsResolved = true;
						mbd.resolvedConstructorArguments = EMPTY_ARGS;
					}
					bw.setBeanInstance(instantiate(beanName, mbd, uniqueCandidate, EMPTY_ARGS));
					return bw;
				}
			}

			//需要解析构造函数和参数值
			boolean autowiring = (chosenCtors != null || mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
			ConstructorArgumentValues resolvedValues = null;

			int minNrOfArgs;
			if (explicitArgs != null) {
				//显式提供的参数值
				minNrOfArgs = explicitArgs.length;
			} else {
				//获取构造函数参数解析后的值
				ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
				resolvedValues = new ConstructorArgumentValues();
				minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
			}

			//排序候选构造函数
			AutowireUtils.sortConstructors(candidates);
			int minTypeDiffWeight = Integer.MAX_VALUE;
			Set<Constructor<?>> ambiguousConstructors = null;
			Deque<UnsatisfiedDependencyException> causes = null;

			for (Constructor<?> candidate : candidates) {
				//遍历构造函数
				int parameterCount = candidate.getParameterCount();

				if (constructorToUse != null && argsToUse != null && argsToUse.length > parameterCount) {
					//已经找到满足要求的贪婪构造函数，不需要再查找了
					break;
				}
				if (parameterCount < minNrOfArgs) {
					//构造函数参数个数不足
					continue;
				}

				ArgumentsHolder argsHolder;
				Class<?>[] paramTypes = candidate.getParameterTypes();
				if (resolvedValues != null) {
					try {
						//创建参数数组
						String[] paramNames = ConstructorPropertiesChecker.evaluate(candidate, parameterCount);
						if (paramNames == null) {
							ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
							if (pnd != null) {
								paramNames = pnd.getParameterNames(candidate);
							}
						}
						argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames, getUserDeclaredConstructor(candidate), autowiring, candidates.length == 1);
					} catch (UnsatisfiedDependencyException ex) {
						if (logger.isTraceEnabled()) {
							logger.trace("忽略bean'" + beanName + "'的构造函数[" + candidate + "]：" + ex);
						}
						//忽略此构造函数，继续查找
						if (causes == null) {
							causes = new ArrayDeque<>(1);
						}
						causes.add(ex);
						continue;
					}
				} else {
					//显式传入参数值时，参数个数必须与构造函数中参数个数一致
					if (parameterCount != explicitArgs.length) {
						continue;
					}
					argsHolder = new ArgumentsHolder(explicitArgs);
				}

				int typeDiffWeight = (mbd.isLenientConstructorResolution() ? argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
				//选择与最佳匹配的构造函数
				if (typeDiffWeight < minTypeDiffWeight) {
					constructorToUse = candidate;
					argsHolderToUse = argsHolder;
					argsToUse = argsHolder.arguments;
					minTypeDiffWeight = typeDiffWeight;
					ambiguousConstructors = null;
				} else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
					//存在多个最佳匹配的构造函数
					if (ambiguousConstructors == null) {
						ambiguousConstructors = new LinkedHashSet<>();
						ambiguousConstructors.add(constructorToUse);
					}
					ambiguousConstructors.add(candidate);
				}
			}

			if (constructorToUse == null) {
				if (causes != null) {
					//构造函数解析异常
					UnsatisfiedDependencyException ex = causes.removeLast();
					for (Exception cause : causes) {
						this.beanFactory.onSuppressedException(cause);
					}
					throw ex;
				}
				//没有找到满足要求的构造函数
				throw new BeanCreationException(mbd.getResourceDescription(), beanName, "无法解析bean类[" + mbd.getBeanClassName() + "]中匹配的构造函数（提示：为避免类型模糊，为简单类型参数指定索引/类型/名称参数）");
			} else if (ambiguousConstructors != null && !mbd.isLenientConstructorResolution()) {
				//有多个最佳匹配的构造函数
				throw new BeanCreationException(mbd.getResourceDescription(), beanName, "在bean类[" + mbd.getBeanClassName() + "]中找到多个具有模糊匹配的构造函数（提示：为避免类型模糊，为简单类型参数指定索引/类型/名称参数）：" + ambiguousConstructors);
			}

			if (explicitArgs == null && argsHolderToUse != null) {
				//缓存构造函数的参数值以便下次使用
				argsHolderToUse.storeCache(mbd, constructorToUse);
			}
		}

		//实例化bean
		Assert.state(argsToUse != null, "未解决的构造函数参数");
		bw.setBeanInstance(instantiate(beanName, mbd, constructorToUse, argsToUse));
		return bw;
	}


	/**
	 * Resolve the prepared arguments stored in the given bean definition.
	 */
	private Object[] resolvePreparedArguments(String beanName, RootBeanDefinition mbd, BeanWrapper bw, Executable executable, Object[] argsToResolve) {

		TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
		TypeConverter converter = (customConverter != null ? customConverter : bw);
		BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this.beanFactory, beanName, mbd, converter);
		Class<?>[] paramTypes = executable.getParameterTypes();

		Object[] resolvedArgs = new Object[argsToResolve.length];
		for (int argIndex = 0; argIndex < argsToResolve.length; argIndex++) {
			Object argValue = argsToResolve[argIndex];
			MethodParameter methodParam = MethodParameter.forExecutable(executable, argIndex);
			if (argValue == autowiredArgumentMarker) {
				argValue = resolveAutowiredArgument(methodParam, beanName, null, converter, true);
			} else if (argValue instanceof BeanMetadataElement) {
				argValue = valueResolver.resolveValueIfNecessary("constructor argument", argValue);
			} else if (argValue instanceof String) {
				argValue = this.beanFactory.evaluateBeanDefinitionString((String) argValue, mbd);
			}
			Class<?> paramType = paramTypes[argIndex];
			try {
				resolvedArgs[argIndex] = converter.convertIfNecessary(argValue, paramType, methodParam);
			} catch (TypeMismatchException ex) {
				throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam), "Could not convert argument value of type [" + ObjectUtils.nullSafeClassName(argValue) + "] to required type [" + paramType.getName() + "]: " + ex.getMessage());
			}
		}
		return resolvedArgs;
	}

	private Object instantiate(String beanName, RootBeanDefinition mbd, Constructor<?> constructorToUse, Object[] argsToUse) {

		try {
			InstantiationStrategy strategy = this.beanFactory.getInstantiationStrategy();
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged((PrivilegedAction<Object>) () -> strategy.instantiate(mbd, beanName, this.beanFactory, constructorToUse, argsToUse), this.beanFactory.getAccessControlContext());
			} else {
				return strategy.instantiate(mbd, beanName, this.beanFactory, constructorToUse, argsToUse);
			}
		} catch (Throwable ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Bean instantiation via constructor failed", ex);
		}
	}

	/**
	 * 将当前bean的构造函数参数解析为已解析的值的对象。
	 * 这可能涉及查找其他bean。
	 * <p>此方法还用于处理静态工厂方法的调用。
	 *
	 * @param beanName       bean的名称
	 * @param mbd            bean定义
	 * @param bw             BeanWrapper实例
	 * @param cargs          构造函数参数值
	 * @param resolvedValues 已解析的构造函数参数值
	 * @return 构造函数最小参数数目
	 */
	private int resolveConstructorArguments(String beanName, RootBeanDefinition mbd, BeanWrapper bw, ConstructorArgumentValues cargs, ConstructorArgumentValues resolvedValues) {

		// 获取自定义类型转换器和转换器
		TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
		TypeConverter converter = (customConverter != null ? customConverter : bw);
		// 创建bean值解析器
		BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this.beanFactory, beanName, mbd, converter);

		// 获取传入构造函数的最小参数数目
		int minNrOfArgs = cargs.getArgumentCount();

		// 处理所有的索引构造函数参数
		for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : cargs.getIndexedArgumentValues()
				.entrySet()) {
			int index = entry.getKey();
			// 如果索引小于0，抛出异常
			if (index < 0) {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Invalid constructor argument index: " + index);
			}
			// 如果索引比最小参数数目还大，更新最小参数数目
			if (index + 1 > minNrOfArgs) {
				minNrOfArgs = index + 1;
			}
			ConstructorArgumentValues.ValueHolder valueHolder = entry.getValue();
			// 如果值已经被转换，加入已解析的参数值中
			if (valueHolder.isConverted()) {
				resolvedValues.addIndexedArgumentValue(index, valueHolder);
			} else {
				// 否则需要解析该值
				Object resolvedValue = valueResolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
				ConstructorArgumentValues.ValueHolder resolvedValueHolder = new ConstructorArgumentValues.ValueHolder(resolvedValue, valueHolder.getType(), valueHolder.getName());
				resolvedValueHolder.setSource(valueHolder);
				resolvedValues.addIndexedArgumentValue(index, resolvedValueHolder);
			}
		}

		// 处理所有的非索引构造函数参数
		for (ConstructorArgumentValues.ValueHolder valueHolder : cargs.getGenericArgumentValues()) {
			if (valueHolder.isConverted()) {
				resolvedValues.addGenericArgumentValue(valueHolder);
			} else {
				Object resolvedValue = valueResolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
				ConstructorArgumentValues.ValueHolder resolvedValueHolder = new ConstructorArgumentValues.ValueHolder(resolvedValue, valueHolder.getType(), valueHolder.getName());
				resolvedValueHolder.setSource(valueHolder);
				resolvedValues.addGenericArgumentValue(resolvedValueHolder);
			}
		}

		// 返回构造函数的最小参数数目
		return minNrOfArgs;
	}

	/**
	 * 根据已解析出的构造方法参数值创建一个参数数组，以便调用构造方法或工厂方法。
	 */
	private ArgumentsHolder createArgumentArray(String beanName, RootBeanDefinition mbd, @Nullable ConstructorArgumentValues resolvedValues, BeanWrapper bw, Class<?>[] paramTypes, @Nullable String[] paramNames, Executable executable, boolean autowiring, boolean fallback) throws UnsatisfiedDependencyException {

		TypeConverter customConverter = this.beanFactory.getCustomTypeConverter(); // 获取自定义类型转换器
		TypeConverter converter = (customConverter != null ? customConverter : bw); // 如果有自定义的类型转换器，就使用它，否则使用BeanWrapper

		ArgumentsHolder args = new ArgumentsHolder(paramTypes.length); // 创建参数数组持有者，用于存储参数数组
		Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = new HashSet<>(paramTypes.length); // 用于存储已使用的构造器参数值的占位符的集合
		Set<String> autowiredBeanNames = new LinkedHashSet<>(4); // 用于存储自动装配的bean名称的集合

		for (int paramIndex = 0; paramIndex < paramTypes.length; paramIndex++) { // 遍历参数类型数组，处理每个参数
			Class<?> paramType = paramTypes[paramIndex]; // 获取当前参数数据类型
			String paramName = (paramNames != null ? paramNames[paramIndex] : ""); // 获取当前参数的名称
			// 尝试查找匹配的构造函数实参值，可以是索引的或通用的
			ConstructorArgumentValues.ValueHolder valueHolder = null; // 当前参数对应的构造函数参数值的占位符值
			if (resolvedValues != null) {
				valueHolder = resolvedValues.getArgumentValue(paramIndex, paramType, paramName, usedValueHolders);
				// 如果找不到直接匹配的值和不应该自动装配，尝试使用下一个通用、无类型的实参值作为备用，因为它在类型转换后（例如，String -> int）可能会匹配。
				if (valueHolder == null && (!autowiring || paramTypes.length == resolvedValues.getArgumentCount())) {
					valueHolder = resolvedValues.getGenericArgumentValue(null, null, usedValueHolders); // 获取通用的值
				}
			}
			if (valueHolder != null) { // 如果匹配到了构造函数参数值的占位符值，就说明找到了匹配的构造函数参数值
				usedValueHolders.add(valueHolder); // 将已使用的构造函数参数值占位符值加入到占位符集合中
				Object originalValue = valueHolder.getValue(); // 获取占位符对应的原始值
				Object convertedValue; // 转换后的值，可能与原始值不同
				if (valueHolder.isConverted()) { // 如果占位符的值已经被转换了就直接使用
					convertedValue = valueHolder.getConvertedValue(); // 获取已经转换后的值
					args.preparedArguments[paramIndex] = convertedValue; // 将转换后的值放入到preparedArguments中
				} else { // 如果占位符的值没有被转换，就需要进行类型转换
					MethodParameter methodParam = MethodParameter.forExecutable(executable, paramIndex); // 创建 MethodParameter 对象，用于获取参数上的 Annotation 以及泛型参数类型等信息
					try {
						convertedValue = converter.convertIfNecessary(originalValue, paramType, methodParam); // 进行类型转换
					} catch (TypeMismatchException ex) { // 如果转换失败
						throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName,
								new InjectionPoint(methodParam),
								"Could not convert argument value of type [" + ObjectUtils.nullSafeClassName(valueHolder.getValue())
										+ "] to required type [" + paramType.getName() + "]: " + ex.getMessage()); // 抛出异常，说明类型转换失败
					}
					Object sourceHolder = valueHolder.getSource();
					if (sourceHolder instanceof ConstructorArgumentValues.ValueHolder) {
						Object sourceValue = ((ConstructorArgumentValues.ValueHolder) sourceHolder).getValue();
						args.resolveNecessary = true;
						args.preparedArguments[paramIndex] = sourceValue;
					}
				}
				args.arguments[paramIndex] = convertedValue; // 将转换后的值放入到 arguments 中
				args.rawArguments[paramIndex] = originalValue; // 将原始值放入到 rawArguments 中
			} else { // 如果没有匹配到对应的构造函数参数值的占位符值，就需要进行自动装配
				MethodParameter methodParam = MethodParameter.forExecutable(executable, paramIndex); // 创建 MethodParameter 对象，用于获取参数上的 Annotation 以及泛型参数类型等信息
				if (!autowiring) { // 如果不应该自动装配，就说明找不到与当前参数类型匹配的 bean，就抛出异常
					throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName,
							new InjectionPoint(methodParam),
							"Ambiguous argument values for parameter of type [" + paramType.getName() + "] - did you specify the correct bean references as arguments?");
				}
				try {
					Object autowiredArgument = resolveAutowiredArgument(methodParam, beanName, autowiredBeanNames, converter, fallback); // 自动装配参数
					args.rawArguments[paramIndex] = autowiredArgument; // 将转换前的原始值放入到rawArguments 中
					args.arguments[paramIndex] = autowiredArgument; // 将转换后的值放入到arguments中
					args.preparedArguments[paramIndex] = autowiredArgumentMarker; // 将autowiredArgumentMaker标记放入到preparedArguments中
					args.resolveNecessary = true;
				} catch (BeansException ex) {
					throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName,
							new InjectionPoint(methodParam), ex);
				}
			}
		}

		for (String autowiredBeanName : autowiredBeanNames) { // 注册对于当前bean的所有autowiredBeanName的依赖关系
			this.beanFactory.registerDependentBean(autowiredBeanName, beanName); // 注册依赖
			if (logger.isDebugEnabled()) {
				logger.debug("Autowiring by type from bean name '" + beanName + "' via " +
						(executable instanceof Constructor ? "constructor" : "factory method") +
						" to bean named '" + autowiredBeanName + "'");
			}
		}

		return args; // 返回参数数组
	}

	/**
	 * 获取构造函数的用户声明构造函数
	 *
	 * @param constructor 当前构造函数对象
	 * @return 用户声明的构造函数对象
	 */
	protected Constructor<?> getUserDeclaredConstructor(Constructor<?> constructor) {
		// 获取声明构造函数的类对象
		Class<?> declaringClass = constructor.getDeclaringClass();
		// 获取当前构造函数对应的用户类
		Class<?> userClass = ClassUtils.getUserClass(declaringClass);
		// 如果当前构造函数对应的类并不是用户定义的类，说明当前类是继承关系下的父类构造函数
		if (userClass != declaringClass) {
			try {
				// 获取用户类中对应的构造函数
				return userClass.getDeclaredConstructor(constructor.getParameterTypes());
			} catch (NoSuchMethodException ex) {
				// 如果用户类中没有对应的构造函数，则忽略该异常，直接使用当前构造函数
			}
		}
		// 如果当前构造函数对应的类就是用户定义的类，则直接返回当前构造函数对象
		return constructor;
	}

	/**
	 * 解析需要自动注入的参数的模板方法
	 *
	 * @param param              要解析的方法参数
	 * @param beanName           指定的bean名称
	 * @param autowiredBeanNames 自动注入的指定bean名称集合
	 * @param typeConverter      类型转换器
	 * @param fallback           是否使用默认的bean
	 * @return 解析后的参数对象
	 * @throws NoUniqueBeanDefinitionException
	 * @throws NoSuchBeanDefinitionException
	 */
	@Nullable
	protected Object resolveAutowiredArgument(MethodParameter param, String beanName, @Nullable Set<String> autowiredBeanNames, TypeConverter typeConverter, boolean fallback) {
		// 获取方法参数的类型
		Class<?> paramType = param.getParameterType();
		// 如果参数类型为 InjectionPoint 类型，则返回当前 InjectionPoint 对象
		if (InjectionPoint.class.isAssignableFrom(paramType)) {
			InjectionPoint injectionPoint = currentInjectionPoint.get();
			if (injectionPoint == null) {
				throw new IllegalStateException("No current InjectionPoint available for " + param);
			}
			return injectionPoint;
		}
		try {
			// 解析方法参数中需要自动注入的依赖对象
			return this.beanFactory.resolveDependency(new DependencyDescriptor(param, true), beanName, autowiredBeanNames, typeConverter);
		} catch (NoUniqueBeanDefinitionException ex) {
			// 如果解析成功的依赖对象有多个，则抛出 NoUniqueBeanDefinitionException 异常
			throw ex;
		} catch (NoSuchBeanDefinitionException ex) {
			if (fallback) {
				// 如果使用默认的bean，则返回一个空的数组/collection/map
				if (paramType.isArray()) {
					return Array.newInstance(paramType.getComponentType(), 0);
				} else if (CollectionFactory.isApproximableCollectionType(paramType)) {
					return CollectionFactory.createCollection(paramType, 0);
				} else if (CollectionFactory.isApproximableMapType(paramType)) {
					return CollectionFactory.createMap(paramType, 0);
				}
			}
			// 如果没有默认的bean，则抛出 NoSuchBeanDefinitionException 异常
			throw ex;
		}
	}


	/**
	 * 如果可能，解析指定bean定义中的工厂方法。
	 * {@link RootBeanDefinition#getResolvedFactoryMethod()} 可以用来检查结果。
	 *
	 * @param mbd 要检查的bean定义
	 */
	public void resolveFactoryMethodIfPossible(RootBeanDefinition mbd) {
		Class<?> factoryClass;
		boolean isStatic;
		// 如果存在工厂bean，则获取工厂bean的类型，并设置isStatic为false；否则获取当前bean的类型，并设置isStatic为true。
		if (mbd.getFactoryBeanName() != null) {
			factoryClass = this.beanFactory.getType(mbd.getFactoryBeanName());
			isStatic = false;
		} else {
			factoryClass = mbd.getBeanClass();
			isStatic = true;
		}
		Assert.state(factoryClass != null, "Unresolvable factory class");
		// 获取工厂类的用户定义类别，对于CGLIB生成的子类，则返回其父类。
		factoryClass = ClassUtils.getUserClass(factoryClass);

		// 获取工厂方法的候选方法集
		Method[] candidates = getCandidateMethods(factoryClass, mbd);
		Method uniqueCandidate = null;
		for (Method candidate : candidates) {
			// 匹配符合条件的工厂方法
			if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate)) {
				if (uniqueCandidate == null) {
					uniqueCandidate = candidate;
				} else if (isParamMismatch(uniqueCandidate, candidate)) {
					uniqueCandidate = null;
					break;
				}
			}
		}
		// 设定匹配到的工厂方法
		mbd.factoryMethodToIntrospect = uniqueCandidate;
	}

	/**
	 * 获取给定类的所有候选方法，并考虑{@link RootBeanDefinition#isNonPublicAccessAllowed()}标记。
	 * 被调用作为确定工厂方法的起点。
	 *
	 * @param factoryClass 工厂类
	 * @param mbd          bean定义
	 * @return 方法数组
	 */
	private Method[] getCandidateMethods(Class<?> factoryClass, RootBeanDefinition mbd) {
		// 如果存在安全管理器，则使用doPrivileged进行方法获取；否则直接获取。
		if (System.getSecurityManager() != null) {
			return AccessController.doPrivileged((PrivilegedAction<Method[]>) () ->
					(mbd.isNonPublicAccessAllowed() ? ReflectionUtils.getAllDeclaredMethods(factoryClass) : factoryClass.getMethods()));
		} else {
			return (mbd.isNonPublicAccessAllowed() ? ReflectionUtils.getAllDeclaredMethods(factoryClass) : factoryClass.getMethods());
		}
	}

	/**
	 * 判断两个方法的参数是否匹配
	 *
	 * @param uniqueCandidate 唯一的方法候选人
	 * @param candidate       另一个方法候选人
	 * @return 如果参数不匹配则返回true，否则返回false
	 */
	private boolean isParamMismatch(Method uniqueCandidate, Method candidate) {
		// 获取唯一的方法候选人的参数个数
		int uniqueCandidateParameterCount = uniqueCandidate.getParameterCount();
		// 获取另一个方法候选人的参数个数
		int candidateParameterCount = candidate.getParameterCount();
		// 判断参数个数是否相同，或者参数类型是否相同
		return (uniqueCandidateParameterCount != candidateParameterCount || !Arrays.equals(uniqueCandidate.getParameterTypes(), candidate.getParameterTypes()));
	}

	/**
	 * 使用一个有命名的工厂方法来实例化bean。如果bean定义参数指定类，而不是“factory-bean”，或者
	 * 在通过依赖注入配置的工厂对象本身中使用一个实例变量，则该方法可以是静态的。
	 * <p>实现需要迭代指定在RootBeanDefinition中的具有名称的静态或实例方法（该方法可以重载）并尝试与参数进行匹配。
	 * 我们没有附加到构造函数参数的类型，因此试错法是唯一的选择。explicitArgs数组可以包含通过相应的getBean方法以编程方式传递的参数值。
	 *
	 * @param beanName     bean的名称
	 * @param mbd          bean的合并定义
	 * @param explicitArgs 通过getBean方法以编程方式传递的参数值，如果没有则为null（->使用bean定义中的构造函数参数值）
	 * @return 用于新实例的BeanWrapper
	 */
	public BeanWrapper instantiateUsingFactoryMethod(String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {

		// 初始化一个 BeanWrapperImpl 对象用于后续处理 Bean 的属性
		BeanWrapperImpl bw = new BeanWrapperImpl();

		// 初始化 BeanWrapperImpl 对象
		this.beanFactory.initBeanWrapper(bw);

		// 定义变量 factoryBean、factoryClass 和 isStatic
		// factoryBean 用于保存工厂 Bean，factoryClass 用于保存工厂 Bean 的 Class，isStatic 标识工厂方法是否为 static
		Object factoryBean;
		Class<?> factoryClass;
		boolean isStatic;

		// 获取 bean 的 factoryBeanName 属性，如果存在工厂 Bean，则根据 factoryBeanName 属性从 BeanFactory 中获取对应的工厂 Bean，
		// 否则使用 bean 的 Class 中的 static 工厂方法创建 bean 实例
		String factoryBeanName = mbd.getFactoryBeanName();
		if (factoryBeanName != null) {
			// 如果 factoryBeanName 指向当前的 beanName，则抛出异常，factory-bean 的引用指向了相同的 bean 定义
			if (factoryBeanName.equals(beanName)) {
				throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName, "factory-bean reference points back to the same bean definition");
			}

			// 从 BeanFactory 中获取工厂 Bean，并根据该 bean 是否为单例进行注册处理
			factoryBean = this.beanFactory.getBean(factoryBeanName);
			if (mbd.isSingleton() && this.beanFactory.containsSingleton(beanName)) {
				throw new ImplicitlyAppearedSingletonException();
			}

			// 如果是单例 Bean，则需要将该 Bean 注册到依赖中
			this.beanFactory.registerDependentBean(factoryBeanName, beanName);

			// 设置 factoryClass、factoryBean 和 isStatic
			factoryClass = factoryBean.getClass();
			isStatic = false;
		} else {
			// 如果没有 factoryBeanName，则判断是否为静态的方式来创建 Bean
			if (!mbd.hasBeanClass()) {
				throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName, "bean definition declares neither a bean class nor a factory-bean reference");
			}

			// 如果是静态 Bean，则 factoryBean 为空
			factoryBean = null;

			// 设置 factoryClass、factoryBean 和 isStatic
			factoryClass = mbd.getBeanClass();
			isStatic = true;
		}


		Method factoryMethodToUse = null; // 定义一个 Method 类型的变量 factoryMethodToUse，初始值为 null
		ArgumentsHolder argsHolderToUse = null; // 定义一个 ArgumentsHolder 类型的变量 argsHolderToUse，初始值为 null
		Object[] argsToUse = null; // 定义一个 Object 类型的数组 argsToUse，初始值为 null

		if (explicitArgs != null) { // 如果参数 explicitArgs 不为空
			argsToUse = explicitArgs; // 将参数 explicitArgs 赋值给变量 argsToUse
		} else { // 如果参数 explicitArgs 为空
			Object[] argsToResolve = null; // 定义一个 Object 类型的数组 argsToResolve，初始值为 null
			synchronized (mbd.constructorArgumentLock) { // 在对象 mbd 的 constructorArgumentLock 的同步块中执行以下操作
				factoryMethodToUse = (Method) mbd.resolvedConstructorOrFactoryMethod; // 将对象 mbd 中已解析的构造方法或工厂方法赋值给变量 factoryMethodToUse
				if (factoryMethodToUse != null && mbd.constructorArgumentsResolved) { // 如果变量 factoryMethodToUse 不为空且对象 mbd 中的构造参数已经解析
					// Found a cached factory method...
					argsToUse = mbd.resolvedConstructorArguments; // 将对象 mbd 中已解析的构造参数赋值给变量 argsToUse
					if (argsToUse == null) { // 如果变量 argsToUse 为空
						argsToResolve = mbd.preparedConstructorArguments; // 将对象 mbd 中准备好的构造参数赋值给变量 argsToResolve
					}
				}
			}
			if (argsToResolve != null) { // 如果变量 argsToResolve 不为空
				argsToUse = resolvePreparedArguments(beanName, mbd, bw, factoryMethodToUse, argsToResolve); // 调用 resolvePreparedArguments 方法解析准备好的构造参数，并将解析后的结果赋值给变量 argsToUse
			}
		}


		if (factoryMethodToUse == null || argsToUse == null) {
			// 需要确定工厂方法...
			// 尝试所有名为该名称的方法，以查看它们是否与给定的参数匹配。
			// 将工厂类转换为用户类
			factoryClass = ClassUtils.getUserClass(factoryClass);

			// 用于存储候选方法的列表
			List<Method> candidates = null;

			if (mbd.isFactoryMethodUnique) {
				// 如果工厂方法是唯一的，就使用已解析的工厂方法
				if (factoryMethodToUse == null) {
					factoryMethodToUse = mbd.getResolvedFactoryMethod();
				}
				if (factoryMethodToUse != null) {
					candidates = Collections.singletonList(factoryMethodToUse);
				}
			}

			// 如果没有唯一的工厂方法，需要使用反射获得所有工厂方法
			if (candidates == null) {
				candidates = new ArrayList<>();
				Method[] rawCandidates = getCandidateMethods(factoryClass, mbd); //获取当前工厂所有方法
				for (Method candidate : rawCandidates) {
					// 判断是否为静态方法
					if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate)) {
						candidates.add(candidate);
					}
				}
			}

			// 如果找到唯一的工厂方法，则直接实例化
			if (candidates.size() == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
				Method uniqueCandidate = candidates.get(0);
				// 判断工厂方法是否有参数
				if (uniqueCandidate.getParameterCount() == 0) {
					mbd.factoryMethodToIntrospect = uniqueCandidate;
					synchronized (mbd.constructorArgumentLock) {
						mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
						mbd.constructorArgumentsResolved = true;
						mbd.resolvedConstructorArguments = EMPTY_ARGS;
					}
					// 用工厂方法实例化bean对象并设置到BeanWrapper
					bw.setBeanInstance(instantiate(beanName, mbd, factoryBean, uniqueCandidate, EMPTY_ARGS));
					return bw;
				}
			}


			// 如果候选构造函数数量大于1（除去不可变的singletonList），则进行排序
			if (candidates.size() > 1) {
				candidates.sort(AutowireUtils.EXECUTABLE_COMPARATOR);
			}

			// 创建一个解析后的构造函数参数值对象
			ConstructorArgumentValues resolvedValues = null;

			// 判断是否需要通过自动装配来解决依赖关系
			boolean autowiring = (mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);

			// 最小类型差异权重初始化为最大值
			int minTypeDiffWeight = Integer.MAX_VALUE;

			// 不清楚的工厂方法集合对象初始化为null
			Set<Method> ambiguousFactoryMethods = null;

			// 定义最小参数数量
			int minNrOfArgs;

			// 如果存在显式指定的构造函数参数，则直接获取该构造函数参数的数量
			if (explicitArgs != null) {
				minNrOfArgs = explicitArgs.length;
			} else {
				// 如果不存在显式指定的构造函数参数，则从BeanDefinition中的构造函数参数中解析参数值
				if (mbd.hasConstructorArgumentValues()) {
					ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
					resolvedValues = new ConstructorArgumentValues();
					minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
				} else {
					// 如果BeanDefinition中没有构造函数参数，则最小参数数量为0
					minNrOfArgs = 0;
				}
			}

			// 定义一个空的队列 causes，用于存储不满足依赖的异常信息。
			Deque<UnsatisfiedDependencyException> causes = null;

			// 循环遍历多个候选方法
			for (Method candidate : candidates) {
				int parameterCount = candidate.getParameterCount(); // 获取当前候选方法的参数数量

				// 如果参数数量不小于规定的最小参数数量，则进入下一步
				if (parameterCount >= minNrOfArgs) {
					ArgumentsHolder argsHolder; // 定义参数持有者

					Class<?>[] paramTypes = candidate.getParameterTypes(); // 获取当前候选方法的参数类型数组
					if (explicitArgs != null) { // 如果已经有了显式传递的参数，说明该方法要被调用，则参数的数量必须和规定的参数数量相等
						// Explicit arguments given -> arguments length must match exactly.
						if (paramTypes.length != explicitArgs.length) { // 如果参数的数量不匹配，则进入下一步循环
							continue;
						}
						argsHolder = new ArgumentsHolder(explicitArgs); // 将显式传递的参数封装到ArgumentsHolder中，方便后续操作
					} else { // 如果没有显式传递参数
						// Resolved constructor arguments: type conversion and/or autowiring necessary.
						try {
							String[] paramNames = null;
							ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
							if (pnd != null) {
								paramNames = pnd.getParameterNames(candidate);
							}
							// 调用方法根据参数类型和bean名称等信息初始化参数
							argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames, candidate, autowiring, candidates.size() == 1);
						} catch (
								UnsatisfiedDependencyException ex) { // 如果初始化参数失败，则把当前的 UnsatisfiedDependencyException 加到队列中，继续循环
							if (logger.isTraceEnabled()) {
								logger.trace("Ignoring factory method [" + candidate + "] of bean '" + beanName + "': " + ex);
							}
							// Swallow and try next overloaded factory method.
							if (causes == null) {
								causes = new ArrayDeque<>(1);
							}
							causes.add(ex);
							continue;
						}
					}

					// 类型差异权重，这里可以理解为计算一下当前方法的参数与候选参数之间的差异
					int typeDiffWeight = (mbd.isLenientConstructorResolution() ? argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));

					// 如果当前方法与之前的方法存在类型差异（差异小的更好），则替换之前的候选方法
					if (typeDiffWeight < minTypeDiffWeight) {
						factoryMethodToUse = candidate; // 当前方法变成要使用的方法
						argsHolderToUse = argsHolder; // 当前参数持有者变为要使用的参数持有者
						argsToUse = argsHolder.arguments; // 当前传递的参数变成要使用的参数
						minTypeDiffWeight = typeDiffWeight; // 类型差异权重变为最小的
						ambiguousFactoryMethods = null; // 必须清空不确定的方法集合
					}
					// 如果存在多个不确定的候选方法
					// 并且当前方法与之前的方法存在类型差异（否则不需要加入不确定的集合）
					// 则将其加入集合
					// 只在非宽松构造方法解析模式下执行此操作，忽略重载的方法（参数签名相同）
					else if (factoryMethodToUse != null && typeDiffWeight == minTypeDiffWeight && !mbd.isLenientConstructorResolution() && paramTypes.length == factoryMethodToUse.getParameterCount() && !Arrays.equals(paramTypes, factoryMethodToUse.getParameterTypes())) {
						if (ambiguousFactoryMethods == null) {
							ambiguousFactoryMethods = new LinkedHashSet<>();
							ambiguousFactoryMethods.add(factoryMethodToUse);
						}
						ambiguousFactoryMethods.add(candidate);
					}
				}
			}


			if (factoryMethodToUse == null || argsToUse == null) {
				// 如果 factoryMethodToUse 和 argsToUse 中有一个为 null，则表示没有找到匹配的工厂方法，需要抛出 BeanCreationException 异常
				// 先处理 causes 列表中的异常，将最后一个拿出来作为异常抛出，其它的异常作为 suppressedException 抛出
				if (causes != null) {
					UnsatisfiedDependencyException ex = causes.removeLast();
					for (Exception cause : causes) {
						this.beanFactory.onSuppressedException(cause);
					}
					throw ex;
				}
				// 如果 causes 为 null，表示异常已经处理完了，那么就根据参数生成一个描述字符串，抛出 BeanCreationException 异常
				List<String> argTypes = new ArrayList<>(minNrOfArgs);
				if (explicitArgs != null) {
					// 如果 explicitArgs 不为 null，表示调用工厂方法时传递了实参，将实参类型的简单类名添加到 argTypes 列表中
					for (Object arg : explicitArgs) {
						argTypes.add(arg != null ? arg.getClass().getSimpleName() : "null");
					}
				} else if (resolvedValues != null) {
					// 如果 explicitArgs 为 null，并且 resolvedValues 不为 null，表示调用工厂方法时使用 Spring 自动生成的参数，将参数的类型添加到 argTypes 列表中
					Set<ValueHolder> valueHolders = new LinkedHashSet<>(resolvedValues.getArgumentCount());
					valueHolders.addAll(resolvedValues.getIndexedArgumentValues().values());
					valueHolders.addAll(resolvedValues.getGenericArgumentValues());
					for (ValueHolder value : valueHolders) {
						String argType = (value.getType() != null ? ClassUtils.getShortName(value.getType()) : (value.getValue() != null ? value.getValue().getClass().getSimpleName() : "null"));
						argTypes.add(argType);
					}
				}
				// 抛出 BeanCreationException 异常
				String argDesc = StringUtils.collectionToCommaDelimitedString(argTypes);
				throw new BeanCreationException(mbd.getResourceDescription(), beanName, "No matching factory method found on class [" + factoryClass.getName() + "]: " + (mbd.getFactoryBeanName() != null ? "factory bean '" + mbd.getFactoryBeanName() + "'; " : "") + "factory method '" + mbd.getFactoryMethodName() + "(" + argDesc + ")'. " + "Check that a method with the specified name " + (minNrOfArgs > 0 ? "and arguments " : "") + "exists and that it is " + (isStatic ? "static" : "non-static") + ".");
			} else if (void.class == factoryMethodToUse.getReturnType()) {
				// 如果工厂方法的返回类型为 void，抛出 BeanCreationException 异常
				throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Invalid factory method '" + mbd.getFactoryMethodName() + "' on class [" + factoryClass.getName() + "]: needs to have a non-void return type!");
			} else if (ambiguousFactoryMethods != null) {
				// 如果 ambiguousFactoryMethods 不为 null，表示有多个工厂方法匹配成功，需要抛出 BeanCreationException 异常
				throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Ambiguous factory method matches found on class [" + factoryClass.getName() + "] " + "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " + ambiguousFactoryMethods);
			}

			// 如果 explicitArgs 为 null，并且 argsHolderToUse 不为 null，则需要缓存 argsHolderToUse 和 factoryMethodToUse
			if (explicitArgs == null && argsHolderToUse != null) {
				mbd.factoryMethodToIntrospect = factoryMethodToUse;
				argsHolderToUse.storeCache(mbd, factoryMethodToUse);
			}

		}

		bw.setBeanInstance(instantiate(beanName, mbd, factoryBean, factoryMethodToUse, argsToUse));
		return bw;
	}

	private Object instantiate(String beanName, RootBeanDefinition mbd, @Nullable Object factoryBean, Method factoryMethod, Object[] args) {

		try {
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged((PrivilegedAction<Object>) () -> this.beanFactory.getInstantiationStrategy()
						.instantiate(mbd, beanName, this.beanFactory, factoryBean, factoryMethod, args), this.beanFactory.getAccessControlContext());
			} else {
				return this.beanFactory.getInstantiationStrategy()
						.instantiate(mbd, beanName, this.beanFactory, factoryBean, factoryMethod, args);
			}
		} catch (Throwable ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Bean instantiation via factory method failed", ex);
		}
	}

	/**
	 * 私有内部类，用于保存参数组合。
	 */
	private static class ArgumentsHolder {

		// 未经过处理的原始参数数组
		public final Object[] rawArguments;

		// 经过处理的参数数组
		public final Object[] arguments;

		// 已准备好的参数数组
		public final Object[] preparedArguments;

		// 是否需要进一步解析参数
		public boolean resolveNecessary = false;

		/**
		 * 通过参数大小构造一个ArgumentsHolder对象并初始化其实例变量数组。
		 *
		 * @param size 参数的大小
		 */
		public ArgumentsHolder(int size) {
			this.rawArguments = new Object[size];
			this.arguments = new Object[size];
			this.preparedArguments = new Object[size];
		}

		/**
		 * 通过给定的参数构造一个ArgumentsHolder对象并初始化其参数数组。
		 *
		 * @param args 给定的参数数组
		 */
		public ArgumentsHolder(Object[] args) {
			this.rawArguments = args;
			this.arguments = args;
			this.preparedArguments = args;
		}

		/**
		 * 获取方法参数的类型差异权重。
		 *
		 * @param paramTypes 方法的参数类型数组
		 * @return 类型差异权重值，较小的值表示更好的匹配
		 */
		public int getTypeDifferenceWeight(Class<?>[] paramTypes) {
			// 如果找到有效的参数组合，则确定类型差异权重。
			// 尝试在转换后的参数和原始参数上执行类型差异权重。
			// 如果原始权重更小，则使用它。
			// 减少原始权重 1024，以使其优于相同的转换权重。
			int typeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.arguments);
			int rawTypeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.rawArguments) - 1024;
			return Math.min(rawTypeDiffWeight, typeDiffWeight);
		}

		/**
		 * 获取方法参数可分配性权重。
		 *
		 * @param paramTypes 方法的参数类型数组
		 * @return 可分配性权重值，较小的值表示更好的匹配
		 */
		public int getAssignabilityWeight(Class<?>[] paramTypes) {
			for (int i = 0; i < paramTypes.length; i++) {
				if (!ClassUtils.isAssignableValue(paramTypes[i], this.arguments[i])) {
					return Integer.MAX_VALUE;
				}
			}
			for (int i = 0; i < paramTypes.length; i++) {
				if (!ClassUtils.isAssignableValue(paramTypes[i], this.rawArguments[i])) {
					return Integer.MAX_VALUE - 512;
				}
			}
			return Integer.MAX_VALUE - 1024;
		}

		/**
		 * 将解析结果存储到RootBeanDefinition对象中。
		 *
		 * @param mbd                        RootBeanDefinition对象
		 * @param constructorOrFactoryMethod 构造函数或工厂方法对象
		 */
		public void storeCache(RootBeanDefinition mbd, Executable constructorOrFactoryMethod) {
			synchronized (mbd.constructorArgumentLock) {
				mbd.resolvedConstructorOrFactoryMethod = constructorOrFactoryMethod;
				mbd.constructorArgumentsResolved = true;
				if (this.resolveNecessary) {
					mbd.preparedConstructorArguments = this.preparedArguments;
				} else {
					mbd.resolvedConstructorArguments = this.arguments;
				}
			}
		}
	}

	/**
	 * Java 6的ConstructorProperties注解的检查委托。
	 */
	private static class ConstructorPropertiesChecker {

		/**
		 * 通过检查ConstructorProperties注解来获取参数名称数组。
		 *
		 * @param candidate  要检查的构造函数对象
		 * @param paramCount 参数数量
		 * @return 参数名称数组
		 */
		@Nullable
		public static String[] evaluate(Constructor<?> candidate, int paramCount) {
			ConstructorProperties cp = candidate.getAnnotation(ConstructorProperties.class);
			if (cp != null) {
				String[] names = cp.value();
				if (names.length != paramCount) {
					throw new IllegalStateException("Constructor annotated with @ConstructorProperties but not " + "corresponding to actual number of parameters (" + paramCount + "): " + candidate);
				}
				return names;
			} else {
				return null;
			}
		}
	}

}
