package indi.shui4.thinking.spring.depencey.injection;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Lazy;

import java.util.Map;
import java.util.Optional;

/**
 * 注解驱动的依赖注入处理过程
 *
 * @author shui4
 */
public class AnnotationDependencyInjectionResolutionDemo {

    @Lazy
//    @Autowired
    private User lazyUser;

    // org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency
    // 必须 (required=true)
    // 实时注入 (eager=true)
    // 通过类型（User.class）
    // 依赖查找（处理）
    // 字段名称（"user"）
    // 是否首要 (primary=true)
    @Autowired // 依赖查找（处理）
    private User user;  // DependencyDescriptor ->

    // 集合类型依赖注入
//    @Autowired
    private Map<String, User> users;

//    @Autowired
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
        applicationContext.close();
    }

}
