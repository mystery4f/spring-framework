package indi.shui4.thinking.spring.resource;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

/**
 * @author shui4
 */
public class EncodedFileSystemResourceDemo {
	public static void main(String[] args) throws IOException {
		String currentJavaFilePath = "D:\\Documents\\Code\\spring-framework\\thinking-in-spring\\src\\main\\java\\indi\\shui4\\thinking\\spring\\resource\\EncodedFileSystemResourceDemo.java";
		File currentJavaFile = new File(currentJavaFilePath);
		// FileSystemResource => WritableResource => Resource
		FileSystemResource fileSystemResource = new FileSystemResource(currentJavaFilePath);
		EncodedResource encodedResource = new EncodedResource(fileSystemResource, "UTF-8");
		// 字符输入流
		// 字符输入流
		try (Reader reader = encodedResource.getReader()) {
			System.out.println(IOUtils.toString(reader));
		}
	}
}
