package indi.shui4.thinking.spring.questions;

import cn.hutool.core.lang.Console;
import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * {@link org.springframework.beans.factory.ObjectFactory} 延迟依赖查找示例
 *
 * @author shui4
 */
public class ObjectFactoryLazyLookupDemo {

	@Autowired
	private ObjectFactory<User> objectFactory;

	@Autowired
	private ObjectProvider<User> objectProvider;

	public static void main(String[] args) {
		final var applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(ObjectFactoryLazyLookupDemo.class);
		applicationContext.refresh();
		final var bean = applicationContext.getBean(ObjectFactoryLazyLookupDemo.class);
		// false
		Console.log("equals:{}", bean.objectFactory == bean.objectProvider);
		// true
		Console.log("class equals:{}", bean.objectFactory.getClass() == bean.objectProvider.getClass());
		// hashcode 一致
		Console.log("objectFactory:{}", bean.objectFactory.getObject().hashCode());
		Console.log("objectProvider:{}", bean.objectProvider.getObject().hashCode());
		Console.log("applicationContext:{}", applicationContext.getBean(User.class).hashCode());

		applicationContext.close();
	}

	@Bean
	public User user() {
		final var user = new User();
		user.setId(1L);
		user.setName("shui4");
		return user;
	}

}
