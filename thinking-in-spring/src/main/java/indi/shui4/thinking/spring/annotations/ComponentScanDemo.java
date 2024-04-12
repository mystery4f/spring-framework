package indi.shui4.thinking.spring.annotations;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * ComponentScanDemo
 *
 * @author shui4
 */
@ComponentScan("indi.shui4.thinking.spring.annotations") // 指定 Class-Path(s)
public class ComponentScanDemo {
	public static void main(String[] args) {
		final var applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(ComponentScanDemo.class);
		applicationContext.refresh();
		System.out.println(((AnnotatedBeanDefinition) applicationContext.getDefaultListableBeanFactory()
				.getBeanDefinition("testBean")).getMetadata().getAnnotationAttributes(MyComponent2.class.getName()));

	}
}
