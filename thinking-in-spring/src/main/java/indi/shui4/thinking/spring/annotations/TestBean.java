package indi.shui4.thinking.spring.annotations;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author shui4
 */
@MyComponent2(value1 = "1")
public class TestBean implements ApplicationContextAware, InitializingBean, BeanNameAware {
	private ApplicationContext applicationContext;
	private String name;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final var autowireCapableBeanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
		final var metadata = ((AnnotatedBeanDefinition) autowireCapableBeanFactory.getBeanDefinition(name)).getMetadata();
		final var annotationAttributes = metadata.getAnnotationAttributes(MyComponent2.class.getName());
		System.out.println(annotationAttributes);
	}

	@Override
	public void setBeanName(String name) {
		this.name = name;
	}
}
