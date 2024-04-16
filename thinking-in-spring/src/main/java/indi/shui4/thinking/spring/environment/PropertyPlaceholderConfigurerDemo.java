package indi.shui4.thinking.spring.environment;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author shui4
 */
public class PropertyPlaceholderConfigurerDemo {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"classpath:/META-INF/placeholders-resolver.xml");
		applicationContext.refresh();
		final var user = applicationContext.getBean(User.class);
		System.out.println(user);
		applicationContext.stop();
		applicationContext.close();
	}
}
