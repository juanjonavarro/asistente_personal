package com.juanjonavarro.asistente.ia;

import com.juanjonavarro.asistente.data.*;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AsistenteIA {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy 'a las' HH:mm");

    @Autowired
    ChatModel chatModel;

    @Autowired
    HechoRepository hechoRepository;

    @Autowired
    MensajeChatRepository mensajeChatRepository;

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    FraseDiariaRepository fraseDiariaRepository;

    @Autowired
    AsistenteRepository asistenteRepository;

    public Pair<String, Boolean> continuarOnboarding(Usuario usuario) {
        List<MensajeChat> mensajes = mensajeChatRepository.findByClientidOrderByTsAsc(usuario.getClientid());

        ChatClient chat = ChatClient.builder(chatModel).build();

        MemoriaTool toolIA = new MemoriaTool(usuario.getClientid(), hechoRepository, "ONBOARD");

        String fechaActual = formatter.format(LocalDateTime.now());

        String mensaje = chat.prompt()
                .system(u -> u.text("""
                    Fecha actual: {fecha}
                    Eres un asistente de onboarding. Tu misión es ayudar a los usuarios a completar su onboarding.
                    Primero debes presentarte y decir que eres "su asistente" y que tienes que hacerle unas
                    preguntas para ser más útil.
                    Para ello debes preguntarle su nombre, su profesión, nombres de los familiares.
                    Haz una pregunta para cada uno de estos datos, pero solo una pregunta cada vez.
                    La pregunta de los familiares repitela hasta que te diga que no quiere añadir a nadie más.
                    Finalmente pregúntale si hay algo más sobre él que le gustaría que supieses.
                    Esta pregunta también repitela hasta que te diga que no quiere añadir nada más.
                    Si el usuario prefiere no contestar a algún dato, no vuelvas a preguntar.
                    Cuando hayas terminado esta entrevista y tengas los datos necesarios
                    registra sus datos haciendo uso de la herramienta disponible
                    y dale las gracias simplemente, no le preguntes nada más.""")
                        .param("fecha", fechaActual))
                .messages(mensajes.stream().map(m -> {
                    if (m.getSender().equals(MensajeChat.SenderType.USER)) {
                        return (Message) new UserMessage(m.getMessage());
                    } else {
                        return (Message) new AssistantMessage(m.getMessage());
                    }
                }).toList())
                .tools(toolIA)
                .call().content();

        return Pair.of(mensaje, toolIA.isOnboardingDone());
    }

    public String responder(Usuario usuario) {

        Asistente asistente = asistenteRepository.findById(usuario.getAsistente()).orElseThrow();

        List<MensajeChat> mensajes = mensajeChatRepository.findByClientidAndIdGreaterThanEqualOrderByTsAsc(usuario.getClientid(), usuario.getIdUltimoSaludo());

        List<Hecho> hechos = hechoRepository.findByClientidOrderByTsAsc(usuario.getClientid());

        ChatClient chat = ChatClient.builder(chatModel).build();

        String fechaActual = formatter.format(LocalDateTime.now());

        MemoriaTool toolIA = new MemoriaTool(usuario.getClientid(), hechoRepository, "CHAT");

        String mensaje = chat.prompt()
                .system(u -> u.text("""
                    Fecha actual: {fecha}
                    Eres un asistente con personalidad. Tu misión es dar una respuesta al usuario siendo útil.
                    Se te asignará un rol, debes hablar como si fueses ese personaje.
                    Se te darán una serie de hechos sobre el usuario. Cada hecho relevante tendrá una fecha o el texto "sin fecha" si es un hecho
                    general sin fecha establecida.
                    Puedes usar estos hechos para personalizar tu respuesta si es adecuado, pero no respondas a esos hechos.
                    Responde brevemente a la pregunta del usuario.
                    Si el usuario te pide que registres algún dato o menciona algún dato personal relevante,
                    registralo haciendo uso de la herramienta disponible.
                    # [ROL]
                    {rol}
                    # [HECHOS]
                    {hechos}
                    """)
                        .param("fecha", fechaActual)
                        .param("rol", asistente.getRol())
                        .param("hechos", hechos.stream()
                                .map(h -> "%s - %s".formatted(
                                        h.getFecha() != null ? formatter.format(h.getFecha()) : "sin fecha",
                                        h.getTexto()
                                )).collect(Collectors.joining("\n")))
                )
                .messages(mensajes.stream().map(m -> {
                    if (m.getSender().equals(MensajeChat.SenderType.USER)) {
                        return (Message) new UserMessage(m.getMessage());
                    } else {
                        return (Message) new AssistantMessage(m.getMessage());
                    }
                }).toList())
                .tools(toolIA)
                .call().content();

        return mensaje;
    }

    public String crearSaludo(Usuario usuario) {
        Asistente asistente = asistenteRepository.findById(usuario.getAsistente()).orElseThrow();

        List<Hecho> hechos = hechoRepository.findByClientidOrderByTsAsc(usuario.getClientid());

        ChatClient chat = ChatClient.builder(chatModel).build();

        String fechaActual = formatter.format(LocalDateTime.now());

        // Obtenemos la frase diaria
        Long fraseId = (Long) jdbcTemplate.queryForMap("select frase_diaria from sistema", Map.of()).get("frase_diaria");
        Optional<FraseDiaria> frase = fraseDiariaRepository.findById(fraseId);
        String fraseTexto;
        String fraseAutor;
        if (frase.isPresent()) {
            fraseTexto = frase.get().getTexto();
            fraseAutor = frase.get().getAutor();
        } else {
            fraseTexto = "No hay frase diaria";
            fraseAutor = "No hay frase diaria";
        }

        String saludo = chat.prompt()
                .user(u -> u.text("""
                    Fecha actual: {fecha}
                    Eres un asistente con personalidad. Tu misión es dar un mensaje de buenos días para una agenda.
                    Se te asignará un rol, debes hablar como si fueses ese personaje.
                    Se te darán una serie de hechos. Cada hecho relevante tendrá una fecha o el texto "sin fecha" si es un hecho
                    general sin fecha establecida.
                    Se te dará también una frase diaria.
                    Debes basar tu mensaje de buenos días saludando personalmente al usuario y comentando los hechos
                    que sean relevantes para hoy y los próximos 7 días. IMPORTANTE: Sólo cosas de hoy y de los próximos 7 días.
                    Después debes mostrar la frase diaria y su autor con un breve comentario sobre ella.
                    Utiliza markdown para dar formato al mensaje, pero sólo *negritas* e _itálicas_.
                    Si creas acotaciones, hazlo usando _(itálicas y entre paréntesis)_.
                    # [ROL]
                    {rol}
                    # [HECHOS]
                    {hechos}
                    # [FRASE DIARIA]
                    {fraseTexto}
                    # [AUTOR FRASE DIARIA]
                    {fraseAutor}
                    """)
                        .param("fecha", fechaActual)
                        .param("rol", asistente.getRol())
                        .param("hechos", hechos.stream()
                                .map(h -> "%s - %s".formatted(
                                        h.getFecha() != null ? formatter.format(h.getFecha()) : "sin fecha",
                                        h.getTexto()
                                )).collect(Collectors.joining("\n")))
                        .param("fraseTexto", fraseTexto)
                        .param("fraseAutor", fraseAutor)
                        .param("fecha", fechaActual)
                        .param("rol", asistente.getRol())
                        .param("hechos", hechos.stream()
                                .map(h -> "%s - %s".formatted(
                                        h.getFecha() != null ? formatter.format(h.getFecha()) : "sin fecha",
                                        h.getTexto()
                                )).collect(Collectors.joining("\n")))
                )
                .call()
                .content();

        return saludo;
    }
}
