package com.juanjonavarro.asistente.data;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class MensajeChat {
    public enum SenderType {
        USER,
        ASSISTANT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    String clientid;

    @Enumerated(EnumType.STRING)
    SenderType sender;

    String message;

    LocalDateTime ts;
}
