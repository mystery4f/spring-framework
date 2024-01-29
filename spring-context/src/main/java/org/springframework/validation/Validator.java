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

package org.springframework.validation;

/**
 * 应用程序特定对象的验证器。
 *
 * <p>此接口完全与任何基础结构或上下文无关；也就是说，它不耦合于仅验证Web层、数据访问层或任何其他层中的对象。
 * 因此，它适用于在应用程序的任何层中使用，并支持将验证逻辑封装为其自身的一流公民。
 *
 * <p>下面是一个简单但完整的{@code Validator}实现，用于验证{@code UserLogin}实例的各种{@link String}属性不为空
 * （即它们不为{@code null}且不完全由空白组成），并且存在任何密码时至少为{@code 'MINIMUM_PASSWORD_LENGTH'}个字符长。
 *
 *
 * <pre class="code">public class UserLoginValidator implements Validator {
 *
 *    private static final int MINIMUM_PASSWORD_LENGTH = 6;
 *
 *    public boolean supports(Class clazz) {
 *       return UserLogin.class.isAssignableFrom(clazz);
 *    }
 *
 *    public void validate(Object target, Errors errors) {
 *       ValidationUtils.rejectIfEmptyOrWhitespace(errors, "userName", "field.required");
 *       ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "field.required");
 *       UserLogin login = (UserLogin) target;
 *       if (login.getPassword() != null
 *             &amp;&amp; login.getPassword().trim().length() &lt; MINIMUM_PASSWORD_LENGTH) {
 *          errors.rejectValue("password", "field.min.length",
 *                new Object[]{Integer.valueOf(MINIMUM_PASSWORD_LENGTH)},
 *                "The password must be at least [" + MINIMUM_PASSWORD_LENGTH + "] characters in length.");
 *       }
 *    }
 * }</pre>
 *
 * <p>See also the Spring reference manual for a fuller discussion of
 * the {@code Validator} interface and its role in an enterprise
 * application.
 *
 * @author Rod Johnson
 * @see SmartValidator
 * @see Errors
 * @see ValidationUtils
 */
public interface Validator {

	/**
	 * 此{@link Validator}是否可以{@link #validate(Object, Errors) 验证}提供的{@code clazz}的实例？
	 * <p>此方法<i>通常</i>实现如下：
	 * <pre class="code">return Foo.class.isAssignableFrom(clazz);</pre>
	 * （其中{@code Foo}是要{@link #validate(Object, Errors) 验证}的实际对象实例的类（或超类）。）
	 *
	 * @param clazz 要询问此{@link Validator}是否可以{@link #validate(Object, Errors) 验证}的{@link Class}
	 * @return 如果此{@link Validator}确实可以{@link #validate(Object, Errors) 验证}提供的{@code clazz}的实例，则返回{@code true}
	 */
	boolean supports(Class<?> clazz);

	/**
	 * 验证提供的{@code target}对象，该对象必须是{@link #supports(Class)}方法通常已经（或将）返回{@code true}的{@link Class}。
	 * <p>提供的{@link Errors errors}实例可用于报告任何导致的验证错误。
	 *
	 * @param target 要验证的对象
	 * @param errors 关于验证过程的上下文状态
	 * @see ValidationUtils
	 */
	void validate(Object target, Errors errors);


}
