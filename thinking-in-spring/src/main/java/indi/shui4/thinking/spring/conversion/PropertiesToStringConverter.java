package indi.shui4.thinking.spring.conversion;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * {@link java.util.Properties} -> {@link String} {@link ConditionalGenericConverter} 实现
 *
 * @author shui4
 * @see java.util.Properties
 * @see ConditionalGenericConverter
 */
public class PropertiesToStringConverter implements ConditionalGenericConverter {
	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return Properties.class.equals(sourceType.getObjectType()) && String.class.equals(targetType.getObjectType());
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Properties.class, String.class));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		final var properties = (Properties) source;
		StringBuilder stringBuilder = new StringBuilder();
		for (Entry<Object, Object> entry : properties.entrySet()) {
			stringBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append(System.lineSeparator());
		}
		return stringBuilder.toString();
	}
}
