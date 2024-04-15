/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

/**
 * A specialization of {@link GenericConversionService} configured by default
 * with converters appropriate for most environments.
 *
 * <p>Designed for direct instantiation but also exposes the static
 * {@link #addDefaultConverters(ConverterRegistry)} utility method for ad-hoc
 * use against any {@code ConverterRegistry} instance.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public class DefaultConversionService extends GenericConversionService {

	@Nullable
	private static volatile DefaultConversionService sharedInstance;


	/**
	 * Create a new {@code DefaultConversionService} with the set of
	 * {@linkplain DefaultConversionService#addDefaultConverters(ConverterRegistry) default converters}.
	 */
	public DefaultConversionService() {
		addDefaultConverters(this);
	}

	/**
	 * Add converters appropriate for most environments.
	 *
	 * @param converterRegistry the registry of converters to add to
	 *                          (must also be castable to ConversionService, e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given ConverterRegistry could not be cast to a ConversionService
	 */
	public static void addDefaultConverters(ConverterRegistry converterRegistry) {
		addScalarConverters(converterRegistry);
		addCollectionConverters(converterRegistry);

		converterRegistry.addConverter(new ByteBufferConverter((ConversionService) converterRegistry));
		converterRegistry.addConverter(new StringToTimeZoneConverter());
		converterRegistry.addConverter(new ZoneIdToTimeZoneConverter());
		converterRegistry.addConverter(new ZonedDateTimeToCalendarConverter());

		converterRegistry.addConverter(new ObjectToObjectConverter());
		converterRegistry.addConverter(new IdToEntityConverter((ConversionService) converterRegistry));
		converterRegistry.addConverter(new FallbackObjectToStringConverter());
		converterRegistry.addConverter(new ObjectToOptionalConverter((ConversionService) converterRegistry));
	}

	/**
	 * 向指定的转换器注册表中添加常见的集合转换器。
	 * 这些转换器支持在数组、集合、映射以及字符串间进行相互转换。
	 *
	 * @param converterRegistry 要添加转换器的注册表，该注册表也需要能够被转换为ConversionService接口的实例，
	 *                          例如是{@link ConfigurableConversionService}的实例。
	 * @throws ClassCastException 如果给定的ConverterRegistry无法被转换为ConversionService实例时抛出。
	 * @since 4.2.3
	 */
	public static void addCollectionConverters(ConverterRegistry converterRegistry) {
		// 将converterRegistry强制转换为ConversionService，以便使用更丰富的转换功能
		ConversionService conversionService = (ConversionService) converterRegistry;
		// 添加数组到集合、集合到数组、数组到数组、集合到集合、映射到映射的转换器
		converterRegistry.addConverter(new ArrayToCollectionConverter(conversionService));
		converterRegistry.addConverter(new CollectionToArrayConverter(conversionService));
		converterRegistry.addConverter(new ArrayToArrayConverter(conversionService));
		converterRegistry.addConverter(new CollectionToCollectionConverter(conversionService));
		converterRegistry.addConverter(new MapToMapConverter(conversionService));
		// 添加数组到字符串、字符串到数组的转换器
		converterRegistry.addConverter(new ArrayToStringConverter(conversionService));
		converterRegistry.addConverter(new StringToArrayConverter(conversionService));
		// 添加数组到对象、对象到数组的转换器
		converterRegistry.addConverter(new ArrayToObjectConverter(conversionService));
		converterRegistry.addConverter(new ObjectToArrayConverter(conversionService));
		// 添加集合到字符串、字符串到集合的转换器
		converterRegistry.addConverter(new CollectionToStringConverter(conversionService));
		converterRegistry.addConverter(new StringToCollectionConverter(conversionService));
		// 添加集合到对象、对象到集合的转换器
		converterRegistry.addConverter(new CollectionToObjectConverter(conversionService));
		converterRegistry.addConverter(new ObjectToCollectionConverter(conversionService));
		// 添加Stream转换器
		converterRegistry.addConverter(new StreamConverter(conversionService));
	}

	/**
	 * 向给定的转换器注册表中添加标量转换器。
	 * 这个方法为各种类型的转换提供了支持，例如从字符串到数字、字符、布尔值、枚举、地区、字符集、货币、属性、UUID的转换，
	 * 以及这些类型到字符串的转换。通过添加这些转换器，可以在不同数据类型间进行灵活的转换。
	 *
	 * @param converterRegistry 转换器注册表，用于注册各种类型的转换器。
	 */
	private static void addScalarConverters(ConverterRegistry converterRegistry) {
		// 数字之间的转换工厂
		converterRegistry.addConverterFactory(new NumberToNumberConverterFactory());

		// 添加字符串到数字的转换工厂，并定义Number到String的通用转换器
		converterRegistry.addConverterFactory(new StringToNumberConverterFactory());
		converterRegistry.addConverter(Number.class, String.class, new ObjectToStringConverter());

		// 添加字符串到字符的转换器及字符到字符串的转换器
		converterRegistry.addConverter(new StringToCharacterConverter());
		converterRegistry.addConverter(Character.class, String.class, new ObjectToStringConverter());

		// 添加数字到字符的转换器及字符到数字的转换工厂
		converterRegistry.addConverter(new NumberToCharacterConverter());
		converterRegistry.addConverterFactory(new CharacterToNumberFactory());

		// 添加字符串到布尔值的转换器及布尔值到字符串的转换器
		converterRegistry.addConverter(new StringToBooleanConverter());
		converterRegistry.addConverter(Boolean.class, String.class, new ObjectToStringConverter());

		// 添加字符串到枚举的转换工厂及枚举到字符串的转换器
		converterRegistry.addConverterFactory(new StringToEnumConverterFactory());
		converterRegistry.addConverter(new EnumToStringConverter((ConversionService) converterRegistry));

		// 添加整数到枚举的转换工厂及枚举到整数的转换器
		converterRegistry.addConverterFactory(new IntegerToEnumConverterFactory());
		converterRegistry.addConverter(new EnumToIntegerConverter((ConversionService) converterRegistry));

		// 添加字符串到地区、地区到字符串的转换器
		converterRegistry.addConverter(new StringToLocaleConverter());
		converterRegistry.addConverter(Locale.class, String.class, new ObjectToStringConverter());

		// 添加字符串到字符集、字符集到字符串的转换器
		converterRegistry.addConverter(new StringToCharsetConverter());
		converterRegistry.addConverter(Charset.class, String.class, new ObjectToStringConverter());

		// 添加字符串到货币、货币到字符串的转换器
		converterRegistry.addConverter(new StringToCurrencyConverter());
		converterRegistry.addConverter(Currency.class, String.class, new ObjectToStringConverter());

		// 添加字符串到属性、属性到字符串的转换器
		converterRegistry.addConverter(new StringToPropertiesConverter());
		converterRegistry.addConverter(new PropertiesToStringConverter());

		// 添加字符串到UUID、UUID到字符串的转换器
		converterRegistry.addConverter(new StringToUUIDConverter());
		converterRegistry.addConverter(UUID.class, String.class, new ObjectToStringConverter());
	}

	/**
	 * 返回一个共享的默认 {@code ConversionService} 实例，按需懒加载构建。
	 * <p><b>注意：</b> 我们强烈建议为了自定义目的构建独立的 {@code ConversionService} 实例。
	 * 此访问器仅作为回退方案，用于需要简单类型强制转换但无法以其他方式访问更长寿的
	 * {@code ConversionService} 实例的代码路径。
	 *
	 * @return 共享的 {@code ConversionService} 实例（永远不为 {@code null}）
	 * @since 4.3.5
	 */
	public static ConversionService getSharedInstance() {
		DefaultConversionService cs = sharedInstance;
		// 判断共享实例是否已经被初始化
		if (cs == null) {
			synchronized (DefaultConversionService.class) {
				cs = sharedInstance;
				// 在同步块内部再次检查，以避免双重初始化
				if (cs == null) {
					cs = new DefaultConversionService();
					sharedInstance = cs;
				}
			}
		}
		return cs;
	}

}
