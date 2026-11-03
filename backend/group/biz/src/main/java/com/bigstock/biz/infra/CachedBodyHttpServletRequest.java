package com.bigstock.biz.infra;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

	private final byte[] body;

	public CachedBodyHttpServletRequest(HttpServletRequest request, int maxBytes) throws IOException {

		super(request);

		if (maxBytes <= 0) {
			throw new IllegalArgumentException("maxBytes must be positive");
		}

		// 限制最多讀取 maxBytes + 1。
		// 避免惡意請求無限制佔用 JVM 記憶體。
		byte[] bytes;

		try (var input = request.getInputStream()) {
			bytes = input.readNBytes(maxBytes + 1);
		}

		if (bytes.length > maxBytes) {
			throw new RequestBodyTooLargeException();
		}

		this.body = bytes;
	}

	public byte[] getCachedBody() {
		return body.clone();
	}

	@Override
	public ServletInputStream getInputStream() {

		ByteArrayInputStream input = new ByteArrayInputStream(body);

		return new ServletInputStream() {

			@Override
			public boolean isFinished() {
				return input.available() == 0;
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setReadListener(ReadListener readListener) {
				if (readListener == null) {
					throw new IllegalArgumentException("ReadListener is required");
				}

				try {
					if (input.available() > 0) {
						readListener.onDataAvailable();
					}

					readListener.onAllDataRead();

				} catch (IOException e) {
					readListener.onError(e);
				}
			}

			@Override
			public int read() {
				return input.read();
			}

			@Override
			public int read(byte[] b, int off, int len) {
				return input.read(b, off, len);
			}
		};
	}

	@Override
	public BufferedReader getReader() {

		String encoding = getCharacterEncoding();

		if (encoding == null) {
			encoding = StandardCharsets.UTF_8.name();
		}

		return new BufferedReader(new InputStreamReader(getInputStream(), java.nio.charset.Charset.forName(encoding)));
	}

	@Override
	public int getContentLength() {
		return body.length;
	}

	@Override
	public long getContentLengthLong() {
		return body.length;
	}

	public static class RequestBodyTooLargeException extends IOException {

		/**
		     * 
		     */
		private static final long serialVersionUID = 1L;

		public RequestBodyTooLargeException() {
			super("REQUEST_BODY_TOO_LARGE");
		}
	}
}
