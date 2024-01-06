package indi.shui4.thinking.spring.metadata;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import indi.shui4.thinking.spring.ioc.overview.enums.City;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

/**
 * @author shui4
 */
@PropertySource(name = "yamlPropertySource", value = "classpath:/META-INF/user.yml", factory = YamlPropertySourceFactory.class)
public class AnnotatedYamlPropertySourceDemo {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(AnnotatedYamlPropertySourceDemo.class);
		applicationContext.refresh();
		User user = applicationContext.getBean(User.class);
		System.out.println(user);
		applicationContext.close();
	}

	@Bean
	public User user(@Value("${user.id}") String id, @Value("${user.name}") String name,
					 @Value("${user.city}") City city
	) {
		User user = new User();
		user.setId(id);
		user.setName(name);
		user.setCity(city);
		return user;
	}


}
