package com.smartlearn.forum;

import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

/** Keep forum errors in English without changing other modules' error contracts. */
@Order(-1)
@RestControllerAdvice(assignableTypes=ForumController.class)
public class ForumErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Request failed.":e.getReason())); }
    @ExceptionHandler({MethodArgumentNotValidException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> invalid(Exception e) { return ResponseEntity.badRequest().body(Map.of("message","Check your input: title 8–180 characters, question details 15–6,000, replies 2–6,000, and at most six tags of up to 28 characters.")); }
    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    ResponseEntity<?> conflict(Exception e) { return ResponseEntity.status(409).body(Map.of("message","This discussion is being updated. Refresh and retry.")); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> failed(Exception e) {
        org.slf4j.LoggerFactory.getLogger(ForumErrors.class).error("Forum request failed ({})",e.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(Map.of("message","The forum could not complete this request. Please retry."));
    }
}
