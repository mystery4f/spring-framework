package indi.shui4.thinking.spring.i18n;

import cn.hutool.core.date.DatePattern;
import org.springframework.util.SocketUtils;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * {@link MessageFormat} Demo
 *
 * @author shui4
 */
public class MessageFormatDemo {
	public static void main(String[] args) {
		int planet = 7;
		String event = "a disturbance in the Force";
		String result = MessageFormat.format("At {1,time,long} on {1,date}, there was {2} on planet {0,number,integer}.", planet, new Date(), event);
		System.out.println(result);
		String pattern = "At {1,time,long} on {1,date}, there was {2} on planet {0,number,integer}.";
		MessageFormat messageFormat = new MessageFormat(pattern);
		Object[] obj = {planet, new Date(), event};
		result = messageFormat.format(obj);
		System.out.println(result);

		// 重置 MessageFormatPattern
		messageFormat.applyPattern("This is a  text : {0}");
		result = messageFormat.format(new Object[]{"Hello,World"});
		System.out.println(result);
		result = messageFormat.format(new Object[]{"Hello,World", 666});
		System.out.println(result);
		messageFormat.applyPattern("This is a  text : {0},{1},{2}");
		messageFormat.format(new Object[]{1});

		// 重置 Locale
		messageFormat.setLocale(Locale.ENGLISH);
		messageFormat.applyPattern(pattern);
		result = messageFormat.format(obj);
		System.out.println(result);

		// 重置 Format
		// 根据参数索引来设置 Pattern
		messageFormat.setFormat(1, new SimpleDateFormat(DatePattern.NORM_DATETIME_PATTERN));
		result = messageFormat.format(obj);
		System.out.println(result);
	}
}
