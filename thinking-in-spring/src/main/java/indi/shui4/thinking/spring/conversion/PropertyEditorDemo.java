package indi.shui4.thinking.spring.conversion;

import java.beans.PropertyEditor;

/**
 * {@link PropertyEditor} 示例
 *
 * @author shui4
 */
public class PropertyEditorDemo {
	public static void main(String[] args) {
		// 模拟 Spring Framework 操作
		final var text = "name=shui4";
		final var stringToPropertiesPropertyEditor = new StringToPropertiesPropertyEditor();
		stringToPropertiesPropertyEditor.setAsText(text);
		System.out.println(stringToPropertiesPropertyEditor.getValue());
		System.out.println(stringToPropertiesPropertyEditor.getAsText());
	}
}
