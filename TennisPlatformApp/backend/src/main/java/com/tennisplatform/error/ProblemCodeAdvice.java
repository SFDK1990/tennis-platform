package com.tennisplatform.error;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;

/**
 * Records the code of every problem a controller advice answers with. One advice here rather than
 * a line in each module's handler: a module that forgot it would count its conflicts as
 * {@code none} and nothing would fail.
 */
@RestControllerAdvice
class ProblemCodeAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ProblemDetail problem && request instanceof ServletServerHttpRequest servlet) {
            Map<String, Object> properties = problem.getProperties();
            if (properties != null && properties.get("code") instanceof String code) {
                ProblemCode.record(servlet.getServletRequest(), code);
            }
        }
        return body;
    }
}
