/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context;

import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * Strategy interface for resolving messages, with support for the parameterization
 * and internationalization of such messages.
 *
 * <p>Spring provides two out-of-the-box implementations for production:
 * <ul>
 * <li>{@link org.springframework.context.support.ResourceBundleMessageSource}: built
 * on top of the standard {@link java.util.ResourceBundle}, sharing its limitations.
 * <li>{@link org.springframework.context.support.ReloadableResourceBundleMessageSource}:
 * highly configurable, in particular with respect to reloading message definitions.
 * </ul>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.context.support.ResourceBundleMessageSource
 * @see org.springframework.context.support.ReloadableResourceBundleMessageSource
 */
public interface MessageSource {

	/**
	 * 尝试解析消息。如果找不到消息，则返回默认消息。
	 *
	 * @param code           要查找的消息代码，例如'calculator.noRateSet'。鼓励MessageSource用户根据合格的类或包名称来命名消息名称，避免潜在的冲突，并确保最大的清晰度。
	 * @param args           将填充消息中的参数的参数数组（参数看起来像消息中的"{0}"、"{1,date}"、"{2,time}"），如果没有则为null
	 * @param defaultMessage 如果查找失败，则返回的默认消息
	 * @param locale         进行查找的区域设置
	 * @return 如果查找成功则返回解析的消息，否则返回作为参数传递的默认消息（可能为null）
	 * @see #getMessage(MessageSourceResolvable, Locale)
	 * @see java.text.MessageFormat
	 */
	@Nullable
	String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale);

	/**
	 * 尝试解析消息。如果找不到消息，则视为错误。
	 *
	 * @param code   要查找的消息代码，例如'calculator.noRateSet'。鼓励MessageSource用户根据合格的类或包名称来命名消息名称，避免潜在的冲突，并确保最大的清晰度。
	 * @param args   将填充消息中的参数的参数数组（参数看起来像消息中的"{0}"、"{1,date}"、"{2,time}"），如果没有则为null
	 * @param locale 进行查找的区域设置
	 * @return 已解析的消息（永远不为null）
	 * @throws NoSuchMessageException 如果未找到对应的消息
	 * @see #getMessage(MessageSourceResolvable, Locale)
	 * @see java.text.MessageFormat
	 */
	String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException;

	/**
	 * 使用传入的MessageSourceResolvable参数中包含的所有属性来尝试解析消息。
	 * 注：由于在调用此方法时我们无法确定resolvable的defaultMessage属性是否为null，因此必须在此方法上抛出NoSuchMessageException。
	 *
	 * @param resolvable 存储解析消息所需属性的值对象（可能包括默认消息）
	 * @param locale     进行查找的区域设置
	 * @return 已解析的消息（永远不为null，即使MessageSourceResolvable提供的默认消息也必须是非null的）
	 * @throws NoSuchMessageException 如果未找到对应的消息（且MessageSourceResolvable未提供默认消息）
	 * @see MessageSourceResolvable#getCodes()
	 * @see MessageSourceResolvable#getArguments()
	 * @see MessageSourceResolvable#getDefaultMessage()
	 * @see java.text.MessageFormat
	 */
	String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException;


}
