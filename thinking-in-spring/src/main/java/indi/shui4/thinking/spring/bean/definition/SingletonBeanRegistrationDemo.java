package indi.shui4.thinking.spring.bean.definition;

import indi.shui4.thinking.spring.bean.factory.DefaultUserFactory;
import indi.shui4.thinking.spring.bean.factory.UserFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 单体 Bean 注册示例
 *
 * @author shui4
 */
public class SingletonBeanRegistrationDemo {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(SingletonBeanRegistrationDemo.class);
        UserFactory userFactory = new DefaultUserFactory();
        applicationContext.getBeanFactory()
                .registerSingleton("userFactory", userFactory);
        applicationContext.refresh();
        System.out.println(applicationContext.getBean("userFactory", UserFactory.class));
        System.out.println(applicationContext.getBean("userFactory", UserFactory.class) == userFactory);
        applicationContext.close();
    }

}
