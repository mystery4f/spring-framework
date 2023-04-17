package indi.shui4.thinking.spring.depencey.injection;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 基于 Api 实现依赖 Setter 方法注入示例
 *
 * @author shui4
 */
public class ApiDependencySetterInjectionDemo {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        // 生成 UserHolder 的 BeanDefinition
        BeanDefinition userHolderBeanDefinition = createUserHolderBeanDefinition();
        // 注册 UserHolder 的 BeanDefinition
        applicationContext.registerBeanDefinition("userHolder", userHolderBeanDefinition);
        // 注册 Configuration Class 配置类
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
        reader.loadBeanDefinitions("classpath:/META-INF/dependency-setter-injection.xml");
        applicationContext.refresh();


        System.out.println(applicationContext.getBean(UserHolder.class));


        applicationContext.close();
    }

    /**
     * 为  {@link UserHolder} 生成 {@link BeanDefinition}
     *
     * @return BeanDefinition
     */
    private static BeanDefinition createUserHolderBeanDefinition() {
        return BeanDefinitionBuilder.genericBeanDefinition(UserHolder.class)
                .addPropertyReference("user", "superUser")
                .setPrimary(true)
                .getBeanDefinition();
    }
  /*  @Bean
    @Primary
    public UserHolder userHolder(User user) {
        return new UserHolder(user);
    }*/

}
