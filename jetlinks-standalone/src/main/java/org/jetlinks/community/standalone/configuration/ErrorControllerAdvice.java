package org.jetlinks.community.standalone.configuration;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.web.ResponseMessage;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrorControllerAdvice {

    @ExceptionHandler
    public Mono<ResponseEntity<ResponseMessage<Object>>> handleException(DeviceOperationException e) {

        //200
        if (e.getCode() == ErrorCode.REQUEST_HANDLING) {
            return Mono.just(ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseMessage.error(200,
                    e.getCode().name().toLowerCase(),
                    e.getMessage())
                    .result("消息已发往设备,处理中...")));
        }
        if (e.getCode() == ErrorCode.FUNCTION_UNDEFINED
            || e.getCode() == ErrorCode.PARAMETER_UNDEFINED) {
            //404
            return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ResponseMessage.error(e.getCode().name().toLowerCase(), e.getMessage())));
        }

        if (e.getCode() == ErrorCode.PARAMETER_UNDEFINED) {
            //400
            return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseMessage.error(e.getCode().name().toLowerCase(), e.getMessage())));
        }

        //500
        return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ResponseMessage.error(e.getCode().name().toLowerCase(), e.getMessage())));
    }


}
