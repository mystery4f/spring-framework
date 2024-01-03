package sun.net.www.protocol.x;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * X {@link URLConnection} 实现
 *
 * @author shui4
 */
public class XURLConnection extends URLConnection {
	private final ClassPathResource classPathResource;

	/**
	 * ignore
	 *
	 * @param url example： x:///META-INF/default.properties
	 */
	protected XURLConnection(URL url) {
		super(url);
		this.classPathResource = new ClassPathResource(url.getPath());
	}

	public static void main(String[] args) throws MalformedURLException {
		URL url = new URL("file:///META-INF/default.properties");
		System.out.println(url.getPath());
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return classPathResource.getInputStream();
	}

	@Override
	public void connect() throws IOException {

	}
}
