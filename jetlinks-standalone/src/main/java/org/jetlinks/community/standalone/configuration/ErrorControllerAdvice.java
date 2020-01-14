package org.jetlinks.community.standalone.configuration;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.web.ResponseMessage;
import org.jetlinks.core.exception.DeviceOperationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrorControllerAdvice {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ResponseMessage<?>> handleException(DeviceOperationException e) {
        return Mono.just(ResponseMessage.error(e.getCode().name().toLowerCase(), e.getMessage()));
    }

}
