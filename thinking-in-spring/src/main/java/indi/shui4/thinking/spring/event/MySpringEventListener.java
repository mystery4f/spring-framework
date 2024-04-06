package indi.shui4.thinking.spring.event;

import cn.hutool.core.lang.Console;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;

/**
 * @author shui4
 */
class MySpringEventListener implements ApplicationListener<MySpringEvent> {

	@Override
	public void onApplicationEvent(@NonNull MySpringEvent event) {
		Console.log("threadName={},threadId={},event={}",
				Thread.currentThread().getName(),
				Thread.currentThread().getId(),
				event
		);
	}
}
