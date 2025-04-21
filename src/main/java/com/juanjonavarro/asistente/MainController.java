package com.juanjonavarro.asistente;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


@RestController
public class MainController {
    @Autowired
    TelegramBot telegramBot;

    @GetMapping("/enviar_diario")
    public String asistente() {
        Thread t = new Thread( () -> telegramBot.enviarMensajeDiario());
        t.start();
        return "enviar_diario";
    }

    @GetMapping("/enviar_diario_usuario")
    public String asistente(@RequestParam String clientId) {
        Thread t = new Thread( () -> {
            try {
                telegramBot.enviarMensajeDiario(clientId);
            } catch (TelegramApiException e) {
                System.out.println("Error enviando mensaje diario a " + clientId);
                System.out.println(e.getLocalizedMessage());
            }
        });
        t.start();
        return "enviar_diario_usuario";
    }

    @GetMapping("/avanzar_dia")
    public String avanzarDia() {
        Thread t = new Thread( () -> telegramBot.avanzarDia());
        t.start();
        return "avanzar_dia";
    }

}
