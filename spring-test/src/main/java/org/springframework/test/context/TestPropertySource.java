/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 该注解用于类级别，用于配置集成测试中 {@link org.springframework.context.ApplicationContext 应用上下文} 的
 * {@link org.springframework.core.env.Environment 环境}的 {@code PropertySources 属性源}。它允许指定属性文件的位置以及内联属性，
 * 这些属性将被添加到应用上下文的属性源中，并且具有比操作系统环境、Java系统属性以及应用通过
 * {@link org.springframework.context.annotation.PropertySource @PropertySource} 或程序（例如，通过
 * {@link org.springframework.context.ApplicationContextInitializer 应用上下文初始化器} 或其他方式）声明添加的属性源更高的优先级。
 *
 * <h3>优先级</h3>
 * <p>测试属性源的优先级高于从操作系统环境或Java系统属性加载的属性源，也高于应用通过
 * {@link org.springframework.context.annotation.PropertySource @PropertySource} 或程序方式声明添加的属性源。因此，测试属性源可以用于选择性地覆盖
 * 系统和应用属性源中定义的属性。此外，通过 {@link DynamicPropertySource @DynamicPropertySource} 注解注册的属性具有比通过
 * {@code @TestPropertySource} 加载的属性更高的优先级。
 *
 * <h3>默认属性文件检测</h3>
 * <p>如果未显式指定 {@link #locations} 或 {@link #properties} 的值，即声明为“空”注解时，将尝试检测一个默认的属性文件，
 * 其路径相对于声明注解的类。例如，如果注解的测试类是 {@code com.example.MyTest}，则相应的默认属性文件是 {@code "classpath:com/example/MyTest.properties"}。
 * 如果无法检测到默认属性文件，将抛出 {@link IllegalStateException} 异常。
 *
 * <h3>启用 &#064;TestPropertySource</h3>
 * <p>如果配置的 {@linkplain ContextConfiguration#loader 上下文加载器} 支持 {@code @TestPropertySource}，则它将被启用。
 * 每个继承自 {@link org.springframework.test.context.support.AbstractGenericContextLoader} 或
 * {@link org.springframework.test.context.web.AbstractGenericWebContextLoader} 的 {@code SmartContextLoader}，
 * 包括 Spring TestContext Framework 提供的每个 {@code SmartContextLoader}，都自动支持 {@code @TestPropertySource}。
 *
 * <h3>其他</h3>
 * <ul>
 * <li>通常，{@code @TestPropertySource} 将与 {@link ContextConfiguration @ContextConfiguration} 一起使用。</li>
 * <li>自 Spring Framework 5.2 起，{@code @TestPropertySource} 可以作为 <em>{@linkplain Repeatable 可重复}</em> 注解使用。</li>
 * <li>此注解可以作为 <em>元注解</em> 使用，用于创建自定义的 <em>组合注解</em>；但是，如果此注解和 {@code @ContextConfiguration}
 * 结合使用，需要小心处理两个注解的 {@code locations} 和 {@code inheritLocations} 属性，以避免在属性解析过程中产生歧义。</li>
 * <li>自 Spring Framework 5.3 起，此注解将默认从包含的测试类继承。详情请参阅 {@link NestedTestConfiguration @NestedTestConfiguration}。</li>
 * </ul>
 *
 * @author Sam Brannen
 * @see ContextConfiguration
 * @see DynamicPropertySource
 * @see org.springframework.core.env.Environment
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.context.annotation.PropertySource
 * @since 4.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(TestPropertySources.class)
public @interface TestPropertySource {

	/**
	 * 该注解属性是{@link #locations}的别名。
	 * <p>这个属性不应当与{@link #locations}同时使用，但可以作为{@link #locations}的替代。
	 *
	 * @return 返回一个字符串数组，默认为空。该属性允许指定一系列的位置信息。
	 * @see #locations
	 */
	@AliasFor("locations")
	String[] value() default {};

	/**
	 * 定义要加载到{@code Environment}的属性文件的资源位置。每个位置将作为其自己的属性源添加到包含的{@code Environment}中，
	 * 并按声明顺序进行添加。
	 * <h3>支持的文件格式</h3>
	 * <p>支持传统和基于XML的属性文件格式 &mdash; 例如, {@code "classpath:/com/example/test.properties"}
	 * 或 {@code "file:/path/to/file.xml"}。
	 * <h3>路径资源语义</h3>
	 * <p>每个路径将被解释为Spring {@link org.springframework.core.io.Resource Resource}。
	 * 平坦的路径 &mdash; 例如, {@code "test.properties"} &mdash; 将被作为类路径资源处理，它是相对于测试类定义的包的。
	 * 路径以斜杠开头将被作为<em>绝对</em>类路径资源处理，例如：{@code "/org/example/test.xml"}。
	 * 指向URL的路径（例如，以{@link org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX 类路径前缀}，
	 * {@link org.springframework.util.ResourceUtils#FILE_URL_PREFIX 文件路径前缀}，{@code http:}等为前缀的路径）
	 * 将使用指定的资源协议进行加载。
	 * 资源位置通配符（例如 <code>*&#42;/*.properties</code>）不被允许：每个位置必须解析为一个{@code .properties}或{@code .xml}资源。
	 * 路径中的属性占位符（即 <code>${...}</code>）将被{@linkplain org.springframework.core.env.Environment#resolveRequiredPlaceholders(String) 解析}
	 * 为{@code Environment}。
	 * <h3>默认属性文件检测</h3>
	 * <p>请参阅类级别Javadoc，了解有关默认值检测的讨论。
	 * <h3>优先级</h3>
	 * <p>从资源位置加载的属性优先级低于内联{@link #properties}中定义的属性。
	 * <p>此属性不能与{@link #value}一起使用，但可以<em>代替</em> {@link #value}。
	 *
	 * @see #inheritLocations
	 * @see #value
	 * @see #properties
	 * @see org.springframework.core.env.PropertySource
	 */
	@AliasFor("value")
	String[] locations() default {};

	/**
	 * Whether or not test property source {@link #locations} from superclasses
	 * should be <em>inherited</em>.
	 * <p>The default value is {@code true}, which means that a test class will
	 * <em>inherit</em> property source locations defined by a superclass.
	 * Specifically, the property source locations for a test class will be
	 * appended to the list of property source locations defined by a superclass.
	 * Thus, subclasses have the option of <em>extending</em> the list of test
	 * property source locations.
	 * <p>If {@code inheritLocations} is set to {@code false}, the property
	 * source locations for the test class will <em>shadow</em> and effectively
	 * replace any property source locations defined by a superclass.
	 * <p>In the following example, the {@code ApplicationContext} for
	 * {@code BaseTest} will be loaded using only the {@code "base.properties"}
	 * file as a test property source. In contrast, the {@code ApplicationContext}
	 * for {@code ExtendedTest} will be loaded using the {@code "base.properties"}
	 * <strong>and</strong> {@code "extended.properties"} files as test property
	 * source locations.
	 * <pre class="code">
	 * &#064;TestPropertySource(&quot;base.properties&quot;)
	 * &#064;ContextConfiguration
	 * public class BaseTest {
	 *   // ...
	 * }
	 *
	 * &#064;TestPropertySource(&quot;extended.properties&quot;)
	 * &#064;ContextConfiguration
	 * public class ExtendedTest extends BaseTest {
	 *   // ...
	 * }</pre>
	 * <p>If {@code @TestPropertySource} is used as a <em>{@linkplain Repeatable
	 * repeatable}</em> annotation, the following special rules apply.
	 * <ol>
	 * <li>All {@code @TestPropertySource} annotations at a given level in the
	 * test class hierarchy (i.e., directly present or meta-present on a test
	 * class) are considered to be <em>local</em> annotations, in contrast to
	 * {@code @TestPropertySource} annotations that are inherited from a
	 * superclass.</li>
	 * <li>All local {@code @TestPropertySource} annotations must declare the
	 * same value for the {@code inheritLocations} flag.</li>
	 * <li>The {@code inheritLocations} flag is not taken into account between
	 * local {@code @TestPropertySource} annotations. Specifically, the property
	 * source locations for one local annotation will be appended to the list of
	 * property source locations defined by previous local annotations. This
	 * allows a local annotation to extend the list of test property source
	 * locations, potentially overriding individual properties.</li>
	 * </ol>
	 *
	 * @see #locations
	 */
	boolean inheritLocations() default true;

	/**
	 * 为 Spring {@link org.springframework.core.env.Environment Environment}添加以<em>键值对</em>形式的<em>内联属性</em>，
	 * 这些属性应该在测试时加载{@code ApplicationContext}之前添加。所有键值对都将作为单个测试{@code PropertySource}添加到包含的{@code Environment}中，具有最高优先级。
	 * <h3>支持的语法</h3>
	 * <p>支持的键值对语法与Java {@linkplain java.util.Properties#load(java.io.Reader) 属性文件}中定义的语法相同：
	 * <ul>
	 * <li>{@code "key=value"}</li>
	 * <li>{@code "key:value"}</li>
	 * <li>{@code "key value"}</li>
	 * </ul>
	 * <h3>优先级</h3>
	 * <p>通过此属性声明的属性具有高于从资源{@link #locations}加载的属性的优先级。
	 * <p>此属性可与{@link #value} <em>或</em> {@link #locations}一起使用。
	 *
	 * @return String数组，代表键值对属性集合。默认为空数组。
	 * @see #inheritProperties
	 * @see #locations
	 * @see org.springframework.core.env.PropertySource
	 */
	String[] properties() default {};

	/**
	 * 是否应该从父类继承内联的测试 {@link #properties}。
	 * <p>默认值为 {@code true}，这意味着测试类将继承由父类定义的内联属性。具体来说，
	 * 测试类的内联属性将附加到父类定义的内联属性列表的末尾。因此，子类有选择地扩展内联测试属性的列表。
	 * <p>如果 {@code inheritProperties} 设置为 {@code false}，则测试类的内联属性将会覆盖并有效地替换父类定义的任何内联属性。
	 * <p>在以下示例中，{@code BaseTest} 的 {@code ApplicationContext} 将仅使用内联的 {@code key1} 属性加载。
	 * 相反，{@code ExtendedTest} 的 {@code ApplicationContext} 将使用内联的 {@code key1} <strong>和</strong> {@code key2} 属性加载。
	 * <pre class="code">
	 * &#064;TestPropertySource(properties = &quot;key1 = value1&quot;)
	 * &#064;ContextConfiguration
	 * public class BaseTest {
	 *   // ...
	 * }
	 * &#064;TestPropertySource(properties = &quot;key2 = value2&quot;)
	 * &#064;ContextConfiguration
	 * public class ExtendedTest extends BaseTest {
	 *   // ...
	 * }</pre>
	 * <p>如果 {@code @TestPropertySource} 用作<em>{@linkplain Repeatable 可重复}</em>注解，则适用以下特殊规则。
	 * <ol>
	 * <li>在测试类层次结构中的给定级别（即直接存在或元存在于测试类上）的所有 {@code @TestPropertySource} 注解被视为<em>本地</em>注解，与从父类继承的 {@code @TestPropertySource} 注解形成对比。</li>
	 * <li>所有本地 {@code @TestPropertySource} 注解必须为 {@code inheritProperties} 标志声明相同的值。</li>
	 * <li>在本地 {@code @TestPropertySource} 注解之间不考虑 {@code inheritProperties} 标志。具体来说，一个本地注解的内联属性将附加到之前本地注解定义的内联属性列表的末尾。这允许本地注解扩展内联属性列表，可能覆盖个别属性。</li>
	 * </ol>
	 * @see #properties
	 */
	boolean inheritProperties() default true;

}
