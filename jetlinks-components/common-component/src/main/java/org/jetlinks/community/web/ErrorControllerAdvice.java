package org.jetlinks.community.web;

import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.exception.DuplicateKeyException;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.web.crud.web.ResponseMessage;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.reference.DataReferenceInfo;
import org.jetlinks.community.reference.DataReferencedException;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrorControllerAdvice {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ResponseMessage<?>> handleException(DecodingException decodingException) {
        Throwable cause = decodingException.getCause();
        if (cause != null) {
            if (cause instanceof JsonMappingException) {
                return handleException(((JsonMappingException) cause));
            }

            return Mono.just(ResponseMessage.error(400, "illegal_argument", cause.getMessage()));
        }
        return Mono.just(ResponseMessage.error(400, "illegal_argument", decodingException.getMessage()));
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ResponseMessage<?>> handleException(JsonMappingException decodingException) {
        Throwable cause = decodingException.getCause();
        if (cause != null) {
            if (cause instanceof IllegalArgumentException) {
                return Mono.just(ResponseMessage.error(400, "illegal_argument", cause.getMessage()));
            }
            return Mono.just(ResponseMessage.error(400, "illegal_argument", cause.getMessage()));
        }
        return Mono.just(ResponseMessage.error(400, "illegal_argument", decodingException.getMessage()));
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ResponseMessage<?>> handleException(DuplicateKeyException e) {
        List<String> columns = e
            .getColumns()
            .stream()
            .map(RDBColumnMetadata::getAlias)
            .collect(Collectors.toList());
        if (columns.isEmpty()) {
            return LocaleUtils
                .resolveMessageReactive("error.duplicate_key")
                .map(msg -> ResponseMessage.error(400, "duplicate_key", msg));
        }
        return LocaleUtils
            .resolveMessageReactive("error.duplicate_key_detail", columns)
            .map(msg -> ResponseMessage.error(400, "duplicate_key", msg).result(columns));
    }

    @ExceptionHandler
    public Mono<ResponseEntity<ResponseMessage<Object>>> handleException(DeviceOperationException e) {

        //200
        if (e.getCode() == ErrorCode.REQUEST_HANDLING) {
            return LocaleUtils
                .resolveMessageReactive("message.device_message_handing")
                .map(msg -> ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ResponseMessage.error(200, "request_handling", msg).result(msg))
                );
        }
        HttpStatus _status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (e.getCode() == ErrorCode.FUNCTION_UNDEFINED
            || e.getCode() == ErrorCode.PARAMETER_UNDEFINED) {
            //404
            _status = HttpStatus.NOT_FOUND;
        } else if (e.getCode() == ErrorCode.PARAMETER_UNDEFINED) {
            //400
            _status = HttpStatus.BAD_REQUEST;
        }

        HttpStatus status = _status;
        return LocaleUtils
            .resolveMessageReactive(e.getCode().getText())
            .map(msg -> ResponseEntity
                .status(status)
                .body(ResponseMessage.error(status.value(), e.getCode().name().toLowerCase(), msg))
            );
    }
    @ExceptionHandler
    public Mono<ResponseMessage<List<DataReferenceInfo>>> handleException(DataReferencedException e) {
        return e
            .getLocalizedMessageReactive()
            .map(msg -> {
                return ResponseMessage
                    .<List<DataReferenceInfo>>error(400,"error.data.referenced", msg)
                    .result(e.getReferenceList());
            });
    }

}
