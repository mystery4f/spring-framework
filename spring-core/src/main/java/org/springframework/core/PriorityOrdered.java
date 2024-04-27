/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core;

/**
 * 此接口扩展自{@link Ordered}接口，表达了优先级排序的含义：{@code PriorityOrdered}对象总是优先于
 * “普通”的{@link Ordered}对象，无论它们的顺序值如何。
 *
 * <p>在排序一组{@code Ordered}对象时，{@code PriorityOrdered}对象和“普通”的{@code Ordered}
 * 对象实际上被视为两个不同的子集，其中{@code PriorityOrdered}对象子集先于“普通”的{@code Ordered}
 * 对象子集，且在这些子集内部应用相对排序。
 *
 * <p>此接口主要用作特殊用途，特别在Spring框架内部，用于需要首先识别“优先级”对象的场景。
 * 一个典型的例子是Spring的{@link org.springframework.context.ApplicationContext}中的优先级后处理器。
 *
 * <p>注意：{@code PriorityOrdered}后处理器 Bean 在一个特殊阶段进行初始化，早于其他后处理器 Bean。
 * 这微妙地影响了它们的自动装配行为：它们仅会与不需要热切初始化来进行类型匹配的 Bean 进行自动装配。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see org.springframework.beans.factory.config.PropertyOverrideConfigurer
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 * @since 2.5
 */
public interface PriorityOrdered extends Ordered {
}

