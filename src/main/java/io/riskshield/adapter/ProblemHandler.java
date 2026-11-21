package io.riskshield.adapter;

import io.riskshield.application.*;
import java.util.NoSuchElementException;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ProblemHandler {
    @ExceptionHandler({IllegalArgumentException.class,HttpMessageNotReadableException.class})
    ProblemDetail invalid(Exception e) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Invalid bounded request or configuration"); }
    @ExceptionHandler({DecisionService.DuplicateConflict.class,ConfigurationService.GovernanceConflict.class,DataIntegrityViolationException.class})
    ProblemDetail conflict(Exception e) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Identity conflict or invalid governance transition"); }
    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail missing(Exception e) { return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,"Resource not found"); }
    @ExceptionHandler({DataAccessException.class,IllegalStateException.class})
    ProblemDetail unavailable(Exception e) { return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,"Durable decision unavailable; retry with the same transaction identity"); }
}
