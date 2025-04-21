package com.juanjonavarro.asistente.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Asistente {
    @Id
    private Long id;

    private String nombre;
    private String rol;
    private String imagen;
}
