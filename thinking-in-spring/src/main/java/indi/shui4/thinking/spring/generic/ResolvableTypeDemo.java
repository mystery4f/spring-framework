package indi.shui4.thinking.spring.generic;

import org.springframework.core.ResolvableType;

/**
 * @author shui4
 */
public class ResolvableTypeDemo {
	public static void main(String[] args) {
		final var resolvableType = ResolvableType.forClass(StringList.class);
		System.out.println("getSuperType="+resolvableType.getSuperType());
		// raw
		System.out.println("asCollection().resolve()="+resolvableType.asCollection().resolve());
		System.out.println("asCollection().resolveGeneric(0)="+resolvableType.asCollection().resolveGeneric(0));
	}
}
