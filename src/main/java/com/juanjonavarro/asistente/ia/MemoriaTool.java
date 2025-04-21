package com.juanjonavarro.asistente.ia;

import com.juanjonavarro.asistente.data.Hecho;
import com.juanjonavarro.asistente.data.HechoRepository;
import org.springframework.ai.tool.annotation.Tool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class MemoriaTool {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private final String clientId;
    private final HechoRepository hechoRepository;
    private boolean onboardingDone = false;
    private final String tipo;

    public MemoriaTool(String clientId, HechoRepository hechoRepository, String tipo) {
        this.clientId = clientId;
        this.hechoRepository = hechoRepository;
        this.tipo = tipo;
    }

    @Tool(description = "Registra la información del usuario")
    public void registraDatosUsuario(List<HechoUsuario> informaciones) {
        System.out.println(informaciones);

        onboardingDone = true;

        List<Map<String, String>> infoMap = (List) informaciones;
        infoMap.forEach(map -> {
            Hecho hecho = new Hecho();
            hecho.setClientid(clientId);
            try {
                if (map.get("fecha") != null) {
                    hecho.setFecha(LocalDateTime.from(formatter.parse(map.get("fecha"))));
                }
            } catch (Exception e) {
                System.out.println(e);
            }

            hecho.setTexto(map.get("hecho"));
            hecho.setTs(LocalDateTime.now());
            hecho.setOrigen(tipo);

            hechoRepository.save(hecho);
        });
    }

    public boolean isOnboardingDone() {
        return onboardingDone;
    }
}
