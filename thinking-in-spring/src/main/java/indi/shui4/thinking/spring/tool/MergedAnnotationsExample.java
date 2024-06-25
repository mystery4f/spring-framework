package indi.shui4.thinking.spring.tool;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.json.JSONUtil;
import indi.shui4.thinking.spring.tool.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationSelectors;
import org.springframework.core.annotation.MergedAnnotations;

/**
 * @author shui4
 */
public class MergedAnnotationsExample {
	@Test
	void searchStrategyByDirect() {
		final var method = ReflectUtil.getMethod(User.class, "print");
		final var mergedAnnotations = MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
		System.out.println(JSONUtil.toJsonStr(mergedAnnotations.get(Bean.class,
						null,
						MergedAnnotationSelectors.firstDirectlyDeclared())
				.asMap(MergedAnnotation.Adapt.ANNOTATION_TO_MAP, MergedAnnotation.Adapt.CLASS_TO_STRING)));
	}
}
