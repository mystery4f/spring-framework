package indi.shui4.thinking.spring.bean.definition;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Bean 别名示例
 *
 * @author shui4
 */
public class BeanAliasDemo {
    public static void main(String[] args) {
        // 配置 XML 配置文件
        // 启动 Sprig 应用上下文
        BeanFactory beanFactory = new ClassPathXmlApplicationContext("classpath:META-INF/bean-definition-context.xml");
        User shui4User = beanFactory.getBean("shui4-user", User.class);
        User user = beanFactory.getBean("user", User.class);
        System.out.println("是否相同:" + (shui4User == user));
    }
}
