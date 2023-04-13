package indi.shui4.thinking.spring.bean.definition;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Bean 实例化 示例
 *
 * @author shui4
 */
public class BeanInstallationDemo {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:META-INF/bean-creation-context.xml");
        System.out.println(applicationContext.getBean("user-by-static-method", User.class));
        System.out.println(applicationContext.getBean("user-by-instance-method", User.class));
        System.out.println(applicationContext.getBean("user-factory-bean", User.class));

    }
}
