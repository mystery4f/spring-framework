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
package indi.shui4.thinking.spring.resource.util;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;

import java.io.IOException;
import java.io.Reader;

/**
 * {@link Resource} 工具类
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public interface ResourceUtils {

	/**
	 * getContent
	 *
	 * @param resource resource
	 * @return String
	 */
	static String getContent(Resource resource) {
		try {
			return getContent(resource, "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * getContent
	 *
	 * @param resource resource
	 * @param encoding encoding
	 * @return String
	 * @throws IOException IOException
	 */
	static String getContent(Resource resource, String encoding) throws IOException {
		EncodedResource encodedResource = new EncodedResource(resource, encoding);
		// 字符输入流
		try (Reader reader = encodedResource.getReader()) {
			return IOUtils.toString(reader);
		}
	}
}
