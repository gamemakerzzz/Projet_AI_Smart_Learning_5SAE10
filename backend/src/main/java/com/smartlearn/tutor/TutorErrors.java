package com.smartlearn.tutor;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Order(-1)
@RestControllerAdvice(assignableTypes=TutorController.class)
public class TutorErrors {
    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    public ResponseEntity<?> conflict(Exception error) {
        return ResponseEntity.status(409).body(Map.of("message","La conversation a changé. Rechargez-la avant de réessayer."));
    }
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> invalid(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("message","Vérifiez les champs : message 4 000 caractères, concept 2 000, texte 16 000, contexte 2 000 ; langues fr, en ou ar."));
    }
}
