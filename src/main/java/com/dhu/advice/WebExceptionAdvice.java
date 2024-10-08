package com.dhu.advice;

import com.dhu.exception.*;
import com.dhu.utils.model.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@ResponseBody
public class WebExceptionAdvice {
    @ExceptionHandler({
            NotMatchException.class,
            MailException.class,
            BlankObjectException.class,
            IllegalObjectException.class,
            HttpException.class,
            OperationException.class,
            NotExistException.class
    })
    public Result doDataException(Exception e) {
        return Result.exception().setMsg(e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public Result doNotLoginException(Exception e) {
        return Result.notLogin().setMsg(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result doAllException(Exception e) {
        return Result.exception().appendMsg(e.toString());
    }
}