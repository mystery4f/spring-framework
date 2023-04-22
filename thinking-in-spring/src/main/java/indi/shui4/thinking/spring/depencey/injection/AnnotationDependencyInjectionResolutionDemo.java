package indi.shui4.thinking.spring.depencey.injection;

import cn.hutool.core.collection.CollUtil;
import indi.shui4.thinking.spring.depencey.injection.annotation.InjectedUser;
import indi.shui4.thinking.spring.depencey.injection.annotation.MyAutowired;
import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

/**
 * 注解驱动的依赖注入处理过程
 *
 * @author shui4
 */
public class AnnotationDependencyInjectionResolutionDemo {


	@InjectedUser
	private User myInjectedUser;

	@Lazy
	@Autowired
	private User lazyUser;

	// org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency
	// 必须 (required=true)
	// 实时注入 (eager=true)
	// 通过类型（User.class）
	// 依赖查找（处理）
	// 字段名称（"user"）
	// 是否首要 (primary=true)
	@Autowired // 依赖查找（处理）
//	@Inject    // JSR-330
	private User user;  // DependencyDescriptor ->

	// 集合类型依赖注入
	@Autowired
	private Map<String, User> users;

	@Autowired
	@MyAutowired
	private Optional<User> userOptional; //supperUser

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(AnnotationDependencyInjectionResolutionDemo.class);

		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
		reader.loadBeanDefinitions("classpath:/META-INF/dependency-lookup-context.xml");

		applicationContext.refresh();
		AnnotationDependencyInjectionResolutionDemo demo = applicationContext.getBean(AnnotationDependencyInjectionResolutionDemo.class);
		System.out.println("demo.user:" + demo.user);
		System.out.println("demo.users:" + demo.users);
		System.out.println("demo.userOptional:" + demo.userOptional);
		System.out.println("demo.lazyUser:" + demo.lazyUser);
		System.out.println("demo.myInjectedUser:" + demo.myInjectedUser);
		applicationContext.close();
	}

	/**
	 * 覆盖 AutowiredAnnotationBeanPostProcessor
	 */
	// 不加 static，需要等待 AnnotationDependencyInjectionResolutionDemo加载之后，
	// 加 static 可以脱离这个限制
	//@Bean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)
	private static AutowiredAnnotationBeanPostProcessor beanPostProcessor() {
		AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor = new AutowiredAnnotationBeanPostProcessor();
		// 额外处理 @InjectedUser 注解
		autowiredAnnotationBeanPostProcessor.setAutowiredAnnotationTypes(
				CollUtil.newLinkedHashSet(Autowired.class, Inject.class, InjectedUser.class)
		);

		return autowiredAnnotationBeanPostProcessor;
	}

	/**
	 * 新增  AutowiredAnnotationBeanPostProcessor 扩展
	 */
	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE - 3)
	// 与 org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.order 一致
	private static AutowiredAnnotationBeanPostProcessor injectedUserAnnotationBeanPostProcessor() {
		AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor = new AutowiredAnnotationBeanPostProcessor();
		autowiredAnnotationBeanPostProcessor.setAutowiredAnnotationType(InjectedUser.class);
		return autowiredAnnotationBeanPostProcessor;
	}
}
