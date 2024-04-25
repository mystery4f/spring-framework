package indi.shui4.thinking.spring.environment;

import cn.hutool.core.lang.Console;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * {@link org.springframework.test.context.TestPropertySource} 示例
 *
 * @author xdc
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "user.name=test", locations = "classpath:/META-INF/test.properties")
//@TestPropertySource(properties = "user.name=test")
//@TestPropertySource(locations = "user.name=test")
@ContextConfiguration(classes = TestPropertySourceTest.class)
public class TestPropertySourceTest {


	@Value("${user.name}")
	private String userName;

	@Autowired
	private ConfigurableEnvironment environment;

	@BeforeEach
	void stetUp() {
		environment.getPropertySources().forEach(propertySource -> {
			System.out.println(propertySource.toString());
		});
	}

	@Test
	void test() {
		Console.log("userName:{}", userName);
	}


}
