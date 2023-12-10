package indi.shui4.thinking.configuration.metadata;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @author shui4
 */
public class UsersNamespaceHandler extends NamespaceHandlerSupport {
	@Override
	public void init() {
		registerBeanDefinitionParser("user", new UsersBeanDefinitionParser());
	}
}
