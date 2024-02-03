package indi.shui4.thinking.spring.validation;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.validation.*;

import java.util.Locale;

/**
 * 自定义 Spring {@link org.springframework.validation.Validator} 示例
 *
 * @author shui4
 */
public class ValidatorDemo {

	public static void main(String[] args) {
		// 1. 创建 UserValidator
		final Validator validator = new UserValidator();
		// 2. 判断是否支持目标对象
		final User user = new User();
		System.out.println("user 对象是否被 UserValidator 支持校验：" + validator.supports(user.getClass()));
		// 3. 创建 Errors 对象
		final Errors errors = new BeanPropertyBindingResult(user, "user");
		validator.validate(user, errors);
		// 4. 获取 MessageSource 对象
		final var messageSource = ErrorsMessageDemo.createMessageSource();
		// 5. 输出所有的错误文案
		for (ObjectError error : errors.getAllErrors()) {
			final var message = messageSource.getMessage(error.getCode(), error.getArguments(), Locale.getDefault());
			System.out.println(message);
		}
	}

	static class UserValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return User.class.isAssignableFrom(clazz);
		}

		@Override
		public void validate(Object target, Errors errors) {
			User user = (User) target;
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "id", "id.required");
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "name.required");
			String userName = user.getName();
			// ...
		}
	}
}
