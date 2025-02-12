/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package indi.shui4.thinking.spring.lifecycle;

import org.springframework.context.Lifecycle;

/**
 * 自定义 {@link Lifecycle} 实现
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since
 */
public class MyLifecycle implements Lifecycle {

	private boolean running = false;

	@Override
	public void start() {
		running = true;
		System.out.println("MyLifecycle 启动...");
	}

	@Override
	public void stop() {
		running = false;
		System.out.println("MyLifecycle 停止...");
	}

	@Override
	public boolean isRunning() {
		return running;
	}
}
