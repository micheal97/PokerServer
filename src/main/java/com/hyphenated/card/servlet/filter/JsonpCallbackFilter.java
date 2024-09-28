/*
 * File take from https://github.com/bhagyas/spring-jsonp-support
 * Copyright Bhagya Nirmaan Silva
 */
package com.hyphenated.card.servlet.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class JsonpCallbackFilter implements Filter {

    private static final Log log = LogFactory.getLog(JsonpCallbackFilter.class);

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        @SuppressWarnings("unchecked")
        Map<String, String[]> parms = httpRequest.getParameterMap();

        if (parms.containsKey("callback")) {
            if (log.isDebugEnabled())
                log.debug("Wrapping response with JSONP callback '" + parms.get("callback")[0] + "'");

            OutputStream out = httpResponse.getOutputStream();

            GenericResponseWrapper wrapper = new GenericResponseWrapper(httpResponse);

            chain.doFilter(request, wrapper);

            out.write((parms.get("callback")[0] + "(").getBytes());
            //Handle error case. If the callback is used, the Exception Handling is skipped
            if (wrapper.getData() == null || wrapper.getData().length == 0) {
                out.write("{\"error\":\"There was a server error completing your request\"}".getBytes());
                wrapper.setStatus(400);
            } else {
                out.write(wrapper.getData());
            }
            out.write(");".getBytes());

            wrapper.setContentType("text/javascript;charset=UTF-8");
            out.close();
        } else {
            chain.doFilter(request, response);
        }
    }

}