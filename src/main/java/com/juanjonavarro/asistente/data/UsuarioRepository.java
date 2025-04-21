package com.juanjonavarro.asistente.data;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    List<Usuario> findByEstado(Usuario.EstadoUsuario estado);
}
