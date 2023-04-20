package indi.shui4.thinking.spring.depencey.injection;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Set;

/**
 * {@link ObjectProvider} 实现延迟注入
 * <p>
 * 被该注解创建的Bean 会被分组。在 spring-cloud LoadBalanced 注解就应用了该注解
 * </p>
 *
 * @author shui4
 */
public class LazyAnnotationDependencyInjectionDemo {

    @Autowired
    private User user;  // 实时注入

    @Autowired
    private ObjectProvider<User> userObjectProvider; // 延迟注入

    @Autowired
    private ObjectFactory<Set<User>> userObjectFactory;

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(LazyAnnotationDependencyInjectionDemo.class);

        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
        reader.loadBeanDefinitions("classpath:/META-INF/dependency-lookup-context.xml");

        applicationContext.refresh();
        LazyAnnotationDependencyInjectionDemo demo = applicationContext.getBean(LazyAnnotationDependencyInjectionDemo.class);
        System.out.println(demo.user);
        System.out.println(demo.userObjectProvider.getObject()); // 继承 ObjectFactory

        demo.userObjectProvider.forEach(System.out::println);
        demo.userObjectFactory.getObject()
                .forEach(System.out::println);
        applicationContext.close();
    }

}
