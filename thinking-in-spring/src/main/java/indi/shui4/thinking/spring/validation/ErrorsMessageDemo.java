package indi.shui4.thinking.spring.validation;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.List;
import java.util.Locale;

/**
 * 错误文案示例
 *
 * @author shui4
 */
public class ErrorsMessageDemo {
	public static void main(String[] args) {
		User user = new User();
		// 1. 选择 Errors 实现 BeanPropertyBindingResult
		Errors errors = new BeanPropertyBindingResult(user, "user");
		// 2. 调用 reject 或 rejectValue
		// reject 生成 ObjectError
		// reject 生成 FieldError
		errors.reject("user.properties.not.null");
		errors.rejectValue("name", "name.required");
		// 3. 获取 FieldError 和 ObjectError
		List<ObjectError> globalErrors = errors.getGlobalErrors();
		// 4. 通过  FieldError 和 ObjectError 的 code 和 args 关联 MessageSource 实现
		List<FieldError> fieldErrors = errors.getFieldErrors();
		List<ObjectError> allErrors = errors.getAllErrors();
		StaticMessageSource messageSource = createMessageSource();
		for (ObjectError error : allErrors) {
			String message = messageSource.getMessage(error.getCode(), error.getArguments(), Locale.getDefault());
			System.out.println(message);
		}

	}

	 static StaticMessageSource createMessageSource() {
		StaticMessageSource staticMessageSource = new StaticMessageSource();
		staticMessageSource.addMessage("user.properties.not.null", Locale.getDefault(), "User 所有属性不能为空");
		staticMessageSource.addMessage("name.required", Locale.getDefault(), "the name of User must be null");
		staticMessageSource.addMessage("id.required", Locale.getDefault(), "the id of User must be null");
		return staticMessageSource;
	}
}
