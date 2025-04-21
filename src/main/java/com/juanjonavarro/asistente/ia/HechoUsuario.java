package com.juanjonavarro.asistente.ia;

import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

@Data
public class HechoUsuario {
    @ToolParam(required = false,description = "Fecha del hecho en formato YYYY/MM/DD HH:MM o nulo si no tiene fecha asociada")
    private String fecha;
    private String hecho;
}
