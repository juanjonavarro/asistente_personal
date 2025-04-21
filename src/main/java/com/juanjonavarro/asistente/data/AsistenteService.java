package com.juanjonavarro.asistente.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AsistenteService {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    AsistenteRepository asistenteRepository;

    public Asistente getAsistenteDia() {
        Long asistenteId = (Long) jdbcTemplate.queryForMap("select asistente_activo from sistema", Map.of()).get("asistente_activo");
        return asistenteRepository.findById(asistenteId).get();
    }
}
