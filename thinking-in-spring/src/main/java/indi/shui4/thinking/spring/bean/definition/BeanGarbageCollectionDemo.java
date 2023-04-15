package indi.shui4.thinking.spring.bean.definition;

import indi.shui4.thinking.spring.bean.factory.DefaultUserFactory;
import indi.shui4.thinking.spring.bean.factory.UserFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Bean垃圾回收（GC）示例
 *
 * @author shui4
 */
public class BeanGarbageCollectionDemo {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(BeanGarbageCollectionDemo.class);
        applicationContext.refresh();
        applicationContext.close();
        System.out.println("Spring 应用上下文已关闭");
        System.gc();
    }

    @Bean(initMethod = "iniUserFactory", destroyMethod = "doDestroy")
//    @Lazy
    public UserFactory userFactory() {
        return new DefaultUserFactory();
    }
}
