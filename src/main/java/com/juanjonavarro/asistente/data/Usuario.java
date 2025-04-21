package com.juanjonavarro.asistente.data;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Usuario {
    public enum EstadoUsuario {
        ONBOARDING,
        ACTIVE
    }

    @Id
    private String clientid;

    private String nombre;
    private Long idUltimoSaludo;

    private Long asistente;

    @Enumerated(EnumType.STRING)
    private EstadoUsuario estado;
}
