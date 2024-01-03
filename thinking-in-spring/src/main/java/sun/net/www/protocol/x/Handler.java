package sun.net.www.protocol.x;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * x协议 {@link URLStreamHandler} 实现
 *
 * @author shui4
 */
public class Handler extends URLStreamHandler {
	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		return new XURLConnection(u);
	}
}
