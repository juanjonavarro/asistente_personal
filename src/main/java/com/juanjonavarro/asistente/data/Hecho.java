package com.juanjonavarro.asistente.data;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Hecho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientid;
    private LocalDateTime fecha;

    private String texto;
    private LocalDateTime ts;
    private String origen;
}
