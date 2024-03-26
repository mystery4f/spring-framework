package indi.shui4.thinking.spring.conversion;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/**
 * String -> Properties {@link java.beans.PropertyEditor}
 *
 * @author shui4
 */
public class StringToPropertiesPropertyEditor extends PropertyEditorSupport implements PropertyEditor {
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		final var properties = new Properties();
		try {
			properties.load(new StringReader(text));
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		setValue(properties);
	}

}
