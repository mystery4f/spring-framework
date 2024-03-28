package indi.shui4.thinking.spring.conversion;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

/**
 * 自定义 {@link PropertyEditorRegistrar} 实现
 *
 * @author shui4
 */
public class CustomizedPropertyEditorRegistrar implements PropertyEditorRegistrar {

	@Override
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		registry.registerCustomEditor(User.class, "context", StringToPropertiesPropertyEditor.INSTANCE);
	}
}

