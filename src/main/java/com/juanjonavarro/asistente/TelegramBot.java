package com.juanjonavarro.asistente;

import com.juanjonavarro.asistente.data.*;
import com.juanjonavarro.asistente.ia.AsistenteIA;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TelegramBot extends TelegramLongPollingBot implements ApplicationRunner {
    private final ExecutorService executor = Executors.newFixedThreadPool(20);

    @Autowired
    AsistenteRepository asistenteRepository;

    @Autowired
    AsistenteService asistenteService;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    MensajeChatRepository mensajeChatRepository;

    @Autowired
    AsistenteIA asistenteIA;

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${telegram.bot.token}")
    private String BOT_TOKEN;

    @Value("${telegram.bot.name")
    private String BOT_USERNAME;

    private volatile boolean shuttingDown = false;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        executor.submit(() -> {
            try {
                // Responder solo si el mensaje contiene texto
                if (update.hasMessage() && update.getMessage().hasText()) {
                    String messageText = update.getMessage().getText();
                    String clientId = String.valueOf(update.getMessage().getChatId());
                    String firstName = update.getMessage().getChat().getFirstName();

                    System.out.printf("Client ID: %s (%s)%n", clientId, firstName);

                    // Comprobamos si el usuario ya está dado de alta
                    Usuario usuario = usuarioRepository.findById(clientId).orElseGet(() -> {
                        Usuario u = new Usuario();
                        u.setClientid(clientId);
                        u.setNombre(firstName);
                        u.setEstado(Usuario.EstadoUsuario.ONBOARDING);
                        u.setIdUltimoSaludo(Long.MAX_VALUE);
                        usuarioRepository.save(u);
                        return u;
                    });

                    if (usuario.getEstado() == Usuario.EstadoUsuario.ONBOARDING) {
                        grabaMensajeChat(usuario, messageText, MensajeChat.SenderType.USER);
                        gestionarOnBoarding(usuario);
                    } else if (usuario.getEstado() == Usuario.EstadoUsuario.ACTIVE) {
                        grabaMensajeChat(usuario, messageText, MensajeChat.SenderType.USER);
                        responderConAsistente(usuario);
                    }
                }
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void responderConAsistente(Usuario usuario) throws TelegramApiException {
        enviarEscribiendo(usuario);
        enviarMensaje(usuario, asistenteIA.responder(usuario));
    }

    private void gestionarOnBoarding(Usuario usuario) throws TelegramApiException {
        enviarEscribiendo(usuario);
        Pair<String, Boolean> onboarding = asistenteIA.continuarOnboarding(usuario);
        long idMensaje = enviarMensaje(usuario, onboarding.getLeft());

        if (onboarding.getRight()) {
            // Si el onboarding ha terminado, cambiamos el estado del usuario
            usuario.setEstado(Usuario.EstadoUsuario.ACTIVE);
            usuario.setIdUltimoSaludo(idMensaje);
            usuario.setAsistente(asistenteService.getAsistenteDia().getId());
            usuarioRepository.save(usuario);

            // Enviamos un mensaje de bienvenida
            enviarMensajeDiario(usuario);
        }
    }

    public long grabaMensajeChat(Usuario usuario, String messageText, MensajeChat.SenderType tipo) {
        MensajeChat m = new MensajeChat();
        m.setClientid(usuario.getClientid());
        m.setMessage(messageText);
        m.setSender(tipo);
        m.setTs(java.time.LocalDateTime.now());

        mensajeChatRepository.save(m);

        return m.getId();
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    public long enviarMensaje(Usuario usuario, String mensaje) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(usuario.getClientid())
                .text(mensaje)
                .parseMode(ParseMode.MARKDOWN)
                .build();
        execute(message);
        return grabaMensajeChat(usuario, message.getText(), MensajeChat.SenderType.ASSISTANT);
    }

    public void enviarMensajeDiario() {
        List<Usuario> usuarios = usuarioRepository.findByEstado(Usuario.EstadoUsuario.ACTIVE);
        for (Usuario u : usuarios) {
            try {
                if (!u.getBloquearAsistente()) {
                    enviarMensajeDiario(u);
                }
            } catch (TelegramApiException e) {
                System.out.println("Error enviando mensaje diario a " + u.getClientid());
                System.out.println(e.getLocalizedMessage());
            }
        }
    }

    public void enviarMensajeDiario(String clientId) throws TelegramApiException {
        Usuario usuario = usuarioRepository.findById(clientId).orElseThrow();
        enviarMensajeDiario(usuario);
    }

    public void enviarMensajeDiario(Usuario usuario) throws TelegramApiException {
        Asistente asistente = asistenteRepository.findById(usuario.getAsistente()).orElseThrow();

        enviarEscribiendo(usuario);

        String textoDiario = asistenteIA.crearSaludo(usuario);

        long idMensaje = enviarMensaje(usuario, "Hoy tu asistente será... %s".formatted(asistente.getNombre()));

        usuario.setIdUltimoSaludo(idMensaje);
        usuarioRepository.save(usuario);

        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(usuario.getClientid())
                .photo(new InputFile(Path.of("imagenes", asistente.getImagen()).toFile()))
                .caption(asistente.getNombre())
                .parseMode(ParseMode.HTML)
                .build();
        execute(sendPhoto);

        enviarMensaje(usuario, textoDiario);
    }

    private void enviarEscribiendo(Usuario usuario) throws TelegramApiException {
        execute(SendChatAction.builder()
                .chatId(usuario.getClientid())
                .action(ActionType.TYPING.name())
                .build());
    }

    @Scheduled(cron = "0 0 10 * * *")
    public void avanzarDia() {
        var rows=jdbcTemplate.queryForList(
            "select id from asistente where id > (select asistente_activo from sistema) order by id limit 1"
                , Map.of());
        if (rows.isEmpty()) {
            rows = jdbcTemplate.queryForList(
                    "select id from asistente where id > 0"
                    , Map.of());
        }

        Long proximoAsistenteId = (Long) rows.getFirst().get("id");

        rows = jdbcTemplate.queryForList(
                "select id from frase_diaria where id > (select frase_diaria from sistema) order by id limit 1"
                , Map.of());
        if (rows.isEmpty()) {
            rows = jdbcTemplate.queryForList(
                    "select id from frase_diaria where id > 0"
                    , Map.of());
        }

        Long proximaFraseId = (Long) rows.getFirst().get("id");

        jdbcTemplate.update("update sistema set asistente_activo = :asistente, frase_diaria = :frase",
                Map.of(
                        "asistente", proximoAsistenteId,
                        "frase"    , proximaFraseId
                ));

        List<Usuario> usuarios = usuarioRepository.findByEstado(Usuario.EstadoUsuario.ACTIVE);
        for (Usuario u : usuarios) {
            try {
                if (!u.getBloquearAsistente()) {
                    u.setAsistente(proximoAsistenteId);
                    usuarioRepository.save(u);
                    enviarMensajeDiario(u);
                }
            } catch (TelegramApiException e) {
                System.out.println("Error enviando mensaje diario a " + u.getClientid());
                System.out.println(e.getLocalizedMessage());
            }
        }
    }

}
