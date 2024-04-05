package indi.shui4.thinking.spring.event;

import org.springframework.context.support.GenericApplicationContext;

/**
 * {@link org.springframework.context.ApplicationListener} 示例
 *
 * @author shui4
 */

public class ApplicationListenerDemo {
	public static void main(String[] args) {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.addApplicationListener(event -> {
			System.out.println("收到事件：" + event.getClass());
		});
		applicationContext.refresh();
		applicationContext.start();
		applicationContext.close();
	}
}
