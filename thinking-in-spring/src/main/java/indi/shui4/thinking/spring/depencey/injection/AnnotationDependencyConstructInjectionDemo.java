package indi.shui4.thinking.spring.depencey.injection;

import indi.shui4.thinking.spring.ioc.overview.domain.User;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 基于 注解的依赖 {@link java.lang.reflect.Constructor} 注入示例
 *
 * @author shui4
 */
public class AnnotationDependencyConstructInjectionDemo {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        // 注册 Configuration Class 配置类
        applicationContext.register(AnnotationDependencyConstructInjectionDemo.class);
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
        reader.loadBeanDefinitions("classpath:/META-INF/dependency-setter-injection.xml");
        applicationContext.refresh();
        System.out.println(applicationContext.getBean(UserHolder.class));
        applicationContext.close();
    }

    @Bean
    @Primary
    public UserHolder userHolder(User user) {
        return new UserHolder(user);
    }

}
