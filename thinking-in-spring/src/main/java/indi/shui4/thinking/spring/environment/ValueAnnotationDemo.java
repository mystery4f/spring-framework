package indi.shui4.thinking.spring.environment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * {@link Value @Value } 注解示例
 *
 * @author xdc
 */
public class ValueAnnotationDemo {

	@Value("${user.name}")
	private String username;

	public static void main(String[] args) {
		final var applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(ValueAnnotationDemo.class);
		applicationContext.refresh();
		System.out.println(applicationContext.getBean(ValueAnnotationDemo.class).username);
		applicationContext.close();
	}


}
