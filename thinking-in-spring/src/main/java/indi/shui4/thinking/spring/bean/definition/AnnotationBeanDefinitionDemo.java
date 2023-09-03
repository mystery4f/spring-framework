package indi.shui4.thinking.spring.bean.definition;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 注册 {@link BeanDefinition} 示例
 *
 * @author shui4
 */
@Import(AnnotationBeanDefinitionDemo.Config.class) // 3. 通过 @Import 来进行导入
public class AnnotationBeanDefinitionDemo {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		// 注册 Configuration Class 配置类
		applicationContext.register(AnnotationBeanDefinitionDemo.class);
		applicationContext.refresh();
		Map<String, Config> configBeans = applicationContext.getBeansOfType(Config.class);
		System.out.println("Config 类型的所有 Beans" + configBeans);
		// 命名 Bean 的注册方式
		registerUserBeanDefinition(applicationContext, "shui4-user");
		// 非命名注册方式
		registerUserBeanDefinition(applicationContext);
		registerUserBeanDefinition(applicationContext);

		Map<String, User> userBeans = applicationContext.getBeansOfType(User.class);
		System.out.println("User 类型的所有 Beans" + userBeans);
		// 显示地关闭 Spring 应用上下文
		applicationContext.close();
	}

	/**
	 * 命名 Bean 的注册方式
	 *
	 * @param registry registry
	 * @param beanName beanName
	 */
	public static void registerUserBeanDefinition(BeanDefinitionRegistry registry, String beanName) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(User.class).addPropertyValue("id", 1L).addPropertyValue("name", "shui4");
		if (StringUtils.hasText(beanName)) {
			// 注册 BeanDefinition
			registry.registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());

		}
		// 非命名 Bean 注册方式
		else {
			BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinitionBuilder.getBeanDefinition(), registry);
		}

	}

	/**
	 * 非命名注册方式
	 *
	 * @param registry registry
	 */
	public static void registerUserBeanDefinition(BeanDefinitionRegistry registry) {
		registerUserBeanDefinition(registry, null);

	}

	// 定义当前类作为 Spring Bean（组件）
	// 2. 通过 @component 方式
	@Component
	public static class Config {

		// 1. 通过 @Bean 的方式定义
		@Bean(name = {"user", "shui4" // 别名
		})
		public User user() {
			User user = new User();
			user.setId("1");
			user.setName("shui4");
			return user;
		}
	}
}
