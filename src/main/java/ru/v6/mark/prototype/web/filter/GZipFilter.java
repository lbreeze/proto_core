package ru.v6.mark.prototype.web.filter;

import org.springframework.web.util.WebUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.util.zip.GZIPOutputStream;

public class GZipFilter implements Filter {
    private static final int BUFFER_SIZE = 1024;
    private static final int MIN_CONTENT_SIZE = 1024;

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        //
        String acceptEncoding = request.getHeader("Accept-Encoding");
        boolean canZip = request.getProtocol().equals("HTTP/1.1") || (acceptEncoding != null && acceptEncoding.contains("gzip"));
        if (canZip) {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream(BUFFER_SIZE);
            ServletResponse wrappedResponse = new HttpServletResponseWrapper(response) {
                private ServletOutputStream outputStream;
                private PrintWriter writer;

                public ServletOutputStream getOutputStream() throws IOException {
                    if (outputStream == null) {
                        outputStream = new ServletOutputStream() {
                            @Override
                            public void write(int b) throws IOException {
                                buffer.write(b);
                            }

                            @Override
                            public void write(byte[] b, int off, int len) throws IOException {
                                buffer.write(b, off, len);
                            }

                            @Override
                            public boolean isReady() {
                                //todo TBD
                                return true;  //To change body of implemented methods use File | Settings | File Templates.
                            }

                            @Override
                            public void setWriteListener(WriteListener writeListener) {
                                //todo TBD
                                //To change body of implemented methods use File | Settings | File Templates.
                            }
                        };
                    }
                    return outputStream;
                }

                public PrintWriter getWriter() throws IOException {
                    if (writer == null) {
                        String characterEncoding = getCharacterEncoding();
                        writer = new PrintWriter(new OutputStreamWriter(buffer, characterEncoding == null ? WebUtils.DEFAULT_CHARACTER_ENCODING : characterEncoding));
                    }
                    return writer;
                }
            };
            //
            filterChain.doFilter(servletRequest, wrappedResponse);
            //
            OutputStream out;
            if (buffer.size() > MIN_CONTENT_SIZE) {
                response.addHeader("Content-Encoding", "gzip");
                response.addHeader("Vary", "Accept-Encoding");
                //
                out = new BufferedOutputStream(new GZIPOutputStream(response.getOutputStream()));
            } else {
                out = new BufferedOutputStream(response.getOutputStream());
            }
            buffer.writeTo(out);
            out.close();
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    public void destroy() {
    }
}
