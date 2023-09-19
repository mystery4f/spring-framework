package indi.shui4.thinking.spring.depencey.injection;

import indi.shui4.thinking.spring.depencey.injection.annotation.UserGroup;
import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Collection;

/**
 * {@link Qualifier} 注解依赖注入。
 * <p>
 * 被该注解创建的Bean 会被分组。在 spring-cloud LoadBalanced 注解就应用了该注解
 * </p>
 *
 * @author shui4
 */
public class QualifierAnnotationDependencyInjectionDemo {

	@Autowired
	private User user;  // superUser -> primary user

	@Autowired
	@Qualifier("user")
	private User namedUser;

	@Autowired
	private Collection<User> allUser; // 2 Beans    user + supperUser

	@Autowired
	@Qualifier
	private Collection<User> qualifierUsers; // 2 Beans user1 + user2

	@Autowired
	@UserGroup
	private Collection<User> groupedUsers;


	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(QualifierAnnotationDependencyInjectionDemo.class);

		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
		reader.loadBeanDefinitions("classpath:/META-INF/dependency-lookup-context.xml");

		applicationContext.refresh();


		QualifierAnnotationDependencyInjectionDemo demo = applicationContext.getBean(QualifierAnnotationDependencyInjectionDemo.class);
		// 期待输入 supperUser Bean
		System.out.println("demo.user:" + demo.user);
		// 期待输入 user Bean
		System.out.println("demo.namedUser:" + demo.namedUser);
		// 期待输出 2 个 Bean superUser user
		System.out.println("demo.allUser.size()：" + demo.allUser.size());
		// 期待输出 4 个 Bean user1 user2
		System.out.println("demo.qualifierUsers.size():" + demo.qualifierUsers.size());
		// 期待输出 2 个 Bean user3 user4
		System.out.println("demo.groupedUsers.size():" + demo.groupedUsers.size());
		applicationContext.close();
	}

	// 整体定义上下文存在 5 个 User 类型的 Bean；
	// supperUser
	// user
	// user1 -> @Qualifier
	// user2 -> @Qualifier
	// user3 -> @UserGroup
	@Bean
	@Qualifier // 进行逻辑分组
	public User user1() {
		return createUser("007");
	}

	private static User createUser(String number) {
		User user = new User();
		user.setId(number);
		return user;
	}

	@Bean
	@Qualifier
	public User user2() {
		return createUser("008");
	}

	@Bean
	@UserGroup
	public User user3() {
		return createUser("userGroup-009");
	}

	@Bean
	@UserGroup
	public User user4() {
		return createUser("usersGroup-010");
	}
}
