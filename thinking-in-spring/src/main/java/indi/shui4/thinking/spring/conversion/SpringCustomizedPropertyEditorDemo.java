package indi.shui4.thinking.spring.conversion;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Spring 自定义 {@link java.beans.PropertyEditor} 示例
 *
 * @author shui4
 */
public class SpringCustomizedPropertyEditorDemo {
	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"classpath:/META-INF/property-editors-context.xml");
		final var user = applicationContext.getBean("user", User.class);
		System.out.println(user);
		System.out.println(user.getContext());
		applicationContext.close();
	}


}
