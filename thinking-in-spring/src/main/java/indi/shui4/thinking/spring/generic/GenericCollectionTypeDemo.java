package indi.shui4.thinking.spring.generic;

import org.springframework.core.ResolvableType;

import java.util.Arrays;
import java.util.List;

/**
 * @author shui4
 */
public class GenericCollectionTypeDemo {

	private static StringList stringList;

	// 这个也属于具体话声明，可以拿到 String
	private static List<String> strings;

	public static void main(String[] args) throws NoSuchFieldException {
		final var superType = ResolvableType.forType(StringList.class).getSuperType();
		System.out.println(Arrays.toString(superType.getGenerics()));
		ResolvableType.forField(GenericCollectionTypeDemo.class.getDeclaredField("strings"),
				GenericCollectionTypeDemo.class
		);

	}
}
