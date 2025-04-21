package com.juanjonavarro.asistente.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class FraseDiaria {
    @Id
    private Long id;

    private String texto;

    private String autor;
}
