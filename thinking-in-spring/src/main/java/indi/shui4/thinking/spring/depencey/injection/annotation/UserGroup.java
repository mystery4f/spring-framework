package indi.shui4.thinking.spring.depencey.injection.annotation;

import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.annotation.*;

/**
 * 用户注解 扩展 {@link Qualifier}
 *
 * @author shui4
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
//@Qualifier("group")
@Qualifier
public @interface UserGroup {
}
