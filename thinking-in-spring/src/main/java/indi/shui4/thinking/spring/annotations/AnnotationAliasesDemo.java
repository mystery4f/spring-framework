package indi.shui4.thinking.spring.annotations;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.type.AnnotationMetadata;

/**
 * AnnotationAliasesDemo
 *
 * @author shui4
 */
//@ComponentScan("indi.shui4.thinking.spring.annotations") // 指定 Class-Path(s)
//@MyComponentScan(scanBasePackages = "indi.shui4.thinking.spring.annotations") // 指定 Class-Path(s)
@MyComponentScan2(scanBasePackages = "indi.shui4.thinking.spring.annotations", basePackages = "6")
// 指定 Class-Path(s)
public class AnnotationAliasesDemo {
	public static void main(String[] args) {
		final var annotations = AnnotationMetadata.introspect(AnnotationAliasesDemo.class).getAnnotations();
		System.out.println("ComponentScan=" + annotations.get(ComponentScan.class.getName())
				.asAnnotationAttributes(Adapt.ANNOTATION_TO_MAP));
		System.out.println("MyComponentScan=" + annotations.get(MyComponentScan.class.getName())
				.asAnnotationAttributes(Adapt.ANNOTATION_TO_MAP));
		// packages 是 scanBasePackages的别名，它们两都有值
		System.out.println("MyComponentScan2=" + annotations.get(MyComponentScan2.class.getName())
				.asAnnotationAttributes(Adapt.ANNOTATION_TO_MAP));

	}
}
