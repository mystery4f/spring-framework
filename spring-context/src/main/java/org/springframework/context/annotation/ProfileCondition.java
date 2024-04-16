/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import org.springframework.core.env.Profiles;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

/**
 * 一个匹配条件类，用于根据{@link Profile @Profile}注解的值进行匹配。
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 4.0
 */
class ProfileCondition implements Condition {

    /**
     * 判断当前条件是否匹配。
     *
     * @param context 提供当前环境和条件上下文的Context对象。
     * @param metadata 提供注解类型元数据的AnnotatedTypeMetadata对象。
     * @return boolean 如果当前环境与{@link Profile @Profile}注解中指定的条件匹配，则返回true；否则返回false。
     */
	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		// 获取所有@Profile注解的属性值
		MultiValueMap<String, Object> attrs = metadata.getAllAnnotationAttributes(Profile.class.getName());
		if (attrs != null) {
			// 遍历value属性，检查当前环境是否接受指定的profile
			for (Object value : attrs.get("value")) {
				if (context.getEnvironment().acceptsProfiles(Profiles.of((String[]) value))) {
					return true;
				}
			}
			return false; // 如果没有匹配的profile，则返回false
		}
		return true; // 如果没有找到@Profile注解，则认为匹配
	}

}
