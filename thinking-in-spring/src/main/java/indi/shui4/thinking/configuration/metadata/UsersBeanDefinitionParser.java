package indi.shui4.thinking.configuration.metadata;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author shui4
 */
public class UsersBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
	@Override
	protected Class<?> getBeanClass(Element element) {
		return User.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		setPropertyValue(element, builder, "id");
		setPropertyValue(element, builder, "name");
		setPropertyValue(element, builder, "city");
	}

	private void setPropertyValue(Element element, BeanDefinitionBuilder builder, String attributeName) {
		String attribute = element.getAttribute(attributeName);
		if (StringUtils.hasText(attribute)) {
			builder.addPropertyValue(attributeName, attribute);
		}
	}
}
