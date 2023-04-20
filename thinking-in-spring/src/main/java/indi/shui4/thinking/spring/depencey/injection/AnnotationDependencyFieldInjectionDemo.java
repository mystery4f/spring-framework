package indi.shui4.thinking.spring.depencey.injection;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.annotation.Resource;

/**
 * 基于 Java 注解的依赖字段注入示例
 *
 * @author shui4
 */
public class AnnotationDependencyFieldInjectionDemo {
    @Autowired
    private UserHolder userHolder1;
    @Resource
    private UserHolder userHolder2;

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        // 注册 Configuration Class 配置类
        applicationContext.register(AnnotationDependencyFieldInjectionDemo.class);
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
        reader.loadBeanDefinitions("classpath:/META-INF/dependency-lookup-context.xml");
        applicationContext.refresh();
        AnnotationDependencyFieldInjectionDemo demo = applicationContext.getBean(AnnotationDependencyFieldInjectionDemo.class);
        System.out.println(demo.userHolder1);
        System.out.println(demo.userHolder2);
        applicationContext.close();
    }

    @Bean
    @Primary
    public UserHolder userHolder(User user) {
        return new UserHolder(user);
    }
}
