package com.leyou.common.advice;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.ExceptionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Date;

@ControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ExceptionResult> handleException(LyException e){
        ExceptionEnum em = e.getExceptionEnum();
        return ResponseEntity.status(em.getStatusCode()).body(new ExceptionResult(em.getStatusCode(),em.getMessage(),new Date()));
    }
}
