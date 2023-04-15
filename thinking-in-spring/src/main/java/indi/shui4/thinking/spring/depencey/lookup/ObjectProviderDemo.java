package indi.shui4.thinking.spring.depencey.lookup;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 通过   {@link ObjectProvider}  作为IoC依赖查找
 *
 * @author shui4
 */
public class ObjectProviderDemo {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(ObjectProviderDemo.class);
        applicationContext.refresh();


        lookupByObjectProvider(applicationContext);

        applicationContext.close();
    }

    private static void lookupByObjectProvider(AnnotationConfigApplicationContext applicationContext) {
        ObjectProvider<String> objectProvider = applicationContext.getBeanProvider(String.class);
        System.out.println(objectProvider.getObject());
    }

    @Bean
    public String helloWorld() {
        return "hello world.";
    }
}
