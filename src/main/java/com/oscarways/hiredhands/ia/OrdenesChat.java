package com.oscarways.hiredhands.ia;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.oscarways.hiredhands.HiredHands;
import com.oscarways.hiredhands.Texto;
import com.oscarways.hiredhands.entity.Mercenario;
import com.oscarways.hiredhands.entity.Oficio;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Órdenes por chat: "Rodrigo, ve a talar aquí". Si el mensaje empieza por el nombre de uno de tus
 * mercenarios (a menos de 64 bloques), se lo manda a Claude junto con su estado; Claude decide qué
 * herramientas usar (seguir, esperar, trabajar, cambiar de oficio) y contesta en personaje.
 *
 * Llama a la API de Anthropic (Messages) por HTTP con java.net.http y Gson, que ya vienen con Minecraft.
 * La llamada va en un hilo aparte para no congelar el servidor; las acciones sobre el mercenario se
 * ejecutan en el hilo del servidor.
 */
@EventBusSubscriber(modid = HiredHands.MODID)
public final class OrdenesChat {
    private OrdenesChat() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final URI API = URI.create("https://api.anthropic.com/v1/messages");
    private static final int MAX_VUELTAS = 3;
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private static final ExecutorService HILO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Hired Hands AI");
        t.setDaemon(true);
        return t;
    });
    /** Mercenarios con una orden en curso: no aceptan otra hasta contestar. */
    /** Mensajes de chat por procesar al final del tick. */
    private static final java.util.Queue<Runnable> PENDIENTES = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final Set<UUID> OCUPADOS = ConcurrentHashMap.newKeySet();
    /** Jugadores a los que ya se avisó de que la IA no tiene clave. */
    private static final Set<UUID> AVISADOS = ConcurrentHashMap.newKeySet();

    private static final java.util.regex.Pattern GRUPO = java.util.regex.Pattern.compile(
            "^(todos|all|everyone|grupo\\s*(\\d)|group\\s*(\\d))\\s*[,:]\\s*(.+)$");

    @SubscribeEvent
    static void alChatear(ServerChatEvent event) {
        ServerPlayer jugador = event.getPlayer();
        String mensaje = event.getRawText();
        MinecraftServer server = jugador.level().getServer();
        // Se procesa al final del tick: así la respuesta sale en el chat después del mensaje del jugador
        // (server.execute lo correría en el acto, antes de que el mensaje se publique).
        PENDIENTES.add(() -> procesar(jugador, mensaje, server));
    }

    @SubscribeEvent
    static void alTerminarTick(ServerTickEvent.Post event) {
        Runnable r;
        while ((r = PENDIENTES.poll()) != null) r.run();
    }

    private static void procesar(ServerPlayer jugador, String mensaje, MinecraftServer server) {
        if (ordenAGrupo(jugador, mensaje)) return;
        Mercenario m = destinatario(jugador, mensaje);
        if (m == null) return;

        // Primero, las órdenes sencillas: sin IA, gratis e instantáneas.
        String orden = sinNombre(m, mensaje);
        Component local = OrdenesLocales.intentar(m, jugador, normalizar(orden));
        if (local != null) {
            recordar(m, orden, local.getString());
            decir(m, jugador, local);
            return;
        }

        if (!ConfigIA.ACTIVADA.get()) return;
        String clave = clave();
        if (clave.isEmpty()) {
            if (AVISADOS.add(jugador.getUUID())) {
                jugador.sendSystemMessage(Texto.t("ia.sin_clave").withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        if (!dentroDelLimite(jugador)) {
            jugador.sendOverlayMessage(m.getName().copy().append(" ").append(Texto.t("ia.respiro")));
            return;
        }
        if (!OCUPADOS.add(m.getUUID())) {
            jugador.sendOverlayMessage(m.getName().copy().append(" ").append(Texto.t("ia.pensando")));
            return;
        }
        String contexto = contexto(m, jugador);
        List<String[]> historial = historial(m);
        HILO.execute(() -> {
            try {
                String respuesta = conversar(clave, m, jugador, server, contexto, historial, orden);
                recordar(m, orden, respuesta);
                server.execute(() -> decir(m, jugador, Component.literal(respuesta)));
            } catch (Exception e) {
                LOGGER.warn("[Hired Hands] La IA falló: {}", e.toString());
                server.execute(() -> jugador.sendSystemMessage(
                        Texto.t("ia.error", m.getName(), String.valueOf(e.getMessage())).withStyle(ChatFormatting.GRAY)));
            } finally {
                OCUPADOS.remove(m.getUUID());
            }
        });
    }

    /**
     * "todos, síganme" / "all, follow me" / "grupo 2, quédense" / "group 2, stay": la misma orden sencilla
     * (sin IA) a todos tus mercenarios cercanos o a los de un grupo (el grupo se pone en su pantalla).
     */
    private static boolean ordenAGrupo(ServerPlayer jugador, String mensaje) {
        java.util.regex.Matcher mt = GRUPO.matcher(normalizar(mensaje));
        if (!mt.matches()) return false;
        String numero = mt.group(2) != null ? mt.group(2) : mt.group(3);
        int grupo = numero != null ? Integer.parseInt(numero) : -1;
        List<Mercenario> lista = jugador.level().getEntitiesOfClass(Mercenario.class,
                jugador.getBoundingBox().inflate(64), m -> m.isOwnedBy(jugador) && (grupo < 0 || m.getGrupo() == grupo));
        if (lista.isEmpty()) {
            jugador.sendSystemMessage(Texto.t(grupo < 0 ? "grupo.nadie" : "grupo.nadie_grupo", grupo).withStyle(ChatFormatting.GRAY));
            return true;
        }
        Component primera = null;
        for (Mercenario m : lista) {
            Component r = OrdenesLocales.intentar(m, jugador, mt.group(4));
            if (r == null) {
                jugador.sendSystemMessage(Texto.t("grupo.solo_sencillas").withStyle(ChatFormatting.GRAY));
                return true;
            }
            if (primera == null) primera = r;
        }
        jugador.sendSystemMessage(Texto.t("grupo.responden", lista.size()).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(": ").withStyle(ChatFormatting.WHITE)).append(primera));
        return true;
    }

    // ---------- a quién le habla ----------

    /** El mercenario propio y cercano cuyo nombre abre el mensaje ("Rodrigo, ..." / "Rodrigo: ..."). */
    private static @Nullable Mercenario destinatario(ServerPlayer jugador, String mensaje) {
        String texto = normalizar(mensaje);
        List<Mercenario> propios = jugador.level().getEntitiesOfClass(Mercenario.class,
                jugador.getBoundingBox().inflate(64), m -> m.isOwnedBy(jugador));
        return propios.stream()
                .filter(m -> {
                    String nombre = normalizar(m.getName().getString().split(" ")[0]);
                    return texto.startsWith(nombre) && texto.length() > nombre.length()
                            && ",: ".indexOf(texto.charAt(nombre.length())) >= 0;
                })
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(jugador)))
                .orElse(null);
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    /** "Rodrigo, ve a talar" -> "ve a talar". */
    private static String sinNombre(Mercenario m, String mensaje) {
        int corte = m.getName().getString().split(" ")[0].length();
        String resto = mensaje.trim();
        resto = resto.length() > corte ? resto.substring(corte) : "";
        return resto.replaceFirst("^[\\s,:]+", "").trim();
    }

    // ---------- memoria de la conversación ----------

    /** Últimos intercambios (mensaje del patrón, respuesta) por mercenario; se olvidan tras 10 minutos sin hablar. */
    private record Charla(List<String[]> turnos, long ultimo) {}

    private static final Map<UUID, Charla> CHARLAS = new ConcurrentHashMap<>();
    private static final int TURNOS_RECORDADOS = 4;
    private static final long OLVIDO_MS = 10 * 60 * 1000;

    private static List<String[]> historial(Mercenario m) {
        Charla c = CHARLAS.get(m.getUUID());
        if (c == null || System.currentTimeMillis() - c.ultimo() > OLVIDO_MS) return List.of();
        return List.copyOf(c.turnos());
    }

    private static void recordar(Mercenario m, String mensaje, String respuesta) {
        List<String[]> turnos = new ArrayList<>(historial(m));
        turnos.add(new String[] {mensaje, respuesta});
        while (turnos.size() > TURNOS_RECORDADOS) turnos.remove(0);
        CHARLAS.put(m.getUUID(), new Charla(turnos, System.currentTimeMillis()));
    }

    // ---------- límite de llamadas ----------

    private static final Map<UUID, java.util.ArrayDeque<Long>> LLAMADAS = new ConcurrentHashMap<>();

    private static boolean dentroDelLimite(ServerPlayer jugador) {
        long ahora = System.currentTimeMillis();
        java.util.ArrayDeque<Long> recientes = LLAMADAS.computeIfAbsent(jugador.getUUID(), k -> new java.util.ArrayDeque<>());
        synchronized (recientes) {
            while (!recientes.isEmpty() && ahora - recientes.peekFirst() > 60_000) recientes.pollFirst();
            if (recientes.size() >= ConfigIA.LLAMADAS_POR_MINUTO.get()) return false;
            recientes.addLast(ahora);
            return true;
        }
    }

    // ---------- conversación con Claude ----------

    private static String clave() {
        String clave = ConfigIA.CLAVE_API.get().trim();
        if (clave.isEmpty()) {
            String entorno = System.getenv("ANTHROPIC_API_KEY");
            clave = entorno != null ? entorno.trim() : "";
        }
        return clave;
    }

    /** Bucle de herramientas: pide, ejecuta las herramientas que pida Claude, devuelve resultados, repite. */
    private static String conversar(String clave, Mercenario m, ServerPlayer jugador, MinecraftServer server,
            String contexto, List<String[]> historial, String mensaje) throws IOException, InterruptedException {
        JsonArray mensajes = new JsonArray();
        // Lo último que se dijeron, para poder seguir una conversación corta.
        for (String[] turno : historial) {
            mensajes.add(mensaje("user", new com.google.gson.JsonPrimitive(turno[0])));
            mensajes.add(mensaje("assistant", new com.google.gson.JsonPrimitive(turno[1])));
        }
        mensajes.add(mensaje("user", new com.google.gson.JsonPrimitive(contexto + "\n\nTu patrón te dice: " + mensaje)));

        for (int vuelta = 0; vuelta < MAX_VUELTAS; vuelta++) {
            JsonObject respuesta = llamar(clave, mensajes);
            String motivo = respuesta.has("stop_reason") && !respuesta.get("stop_reason").isJsonNull()
                    ? respuesta.get("stop_reason").getAsString() : "";
            if ("refusal".equals(motivo)) return "Eso no lo voy a hacer.";
            JsonArray contenido = respuesta.getAsJsonArray("content");
            List<JsonObject> usos = new ArrayList<>();
            for (JsonElement b : contenido) {
                if ("tool_use".equals(b.getAsJsonObject().get("type").getAsString())) usos.add(b.getAsJsonObject());
            }
            if (!"tool_use".equals(motivo) || usos.isEmpty()) return texto(contenido);

            // La respuesta de Claude vuelve tal cual (con sus bloques de pensamiento) antes de los resultados.
            mensajes.add(mensaje("assistant", contenido));
            JsonArray resultados = new JsonArray();
            for (JsonObject uso : usos) {
                String nombre = uso.get("name").getAsString();
                JsonObject entrada = uso.getAsJsonObject("input");
                String resultado = server.submit(() -> ejecutar(m, jugador, nombre, entrada)).join();
                JsonObject r = new JsonObject();
                r.addProperty("type", "tool_result");
                r.addProperty("tool_use_id", uso.get("id").getAsString());
                r.addProperty("content", resultado);
                if (resultado.startsWith("ERROR")) r.addProperty("is_error", true);
                resultados.add(r);
            }
            mensajes.add(mensaje("user", resultados));
        }
        return "A la orden.";
    }

    private static JsonObject llamar(String clave, JsonArray mensajes) throws IOException, InterruptedException {
        String modelo = ConfigIA.MODELO.get().trim();
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("model", modelo);
        cuerpo.addProperty("max_tokens", 4096);
        cuerpo.addProperty("system", SISTEMA);
        cuerpo.add("tools", HERRAMIENTAS);
        cuerpo.add("messages", mensajes);
        // Cada modelo acepta opciones distintas: Haiku 4.5 no admite "effort"; los reintentos automáticos
        // ante un rechazo ("fallbacks") son para Claude Opus 5 y Fable.
        if (!modelo.contains("haiku")) {
            JsonObject salida = new JsonObject();
            salida.addProperty("effort", "low");
            cuerpo.add("output_config", salida);
        }
        boolean conReintento = modelo.startsWith("claude-opus-5") || modelo.startsWith("claude-fable");
        if (conReintento) {
            cuerpo.addProperty("fallbacks", "default");
        }

        HttpRequest.Builder peticion = HttpRequest.newBuilder(API)
                .timeout(Duration.ofSeconds(60))
                .header("content-type", "application/json")
                .header("x-api-key", clave)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(cuerpo)));
        if (conReintento) {
            peticion.header("anthropic-beta", "server-side-fallback-2026-07-01");
        }
        HttpResponse<String> http = HTTP.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
        JsonObject respuesta = JsonParser.parseString(http.body()).getAsJsonObject();
        if (http.statusCode() != 200) {
            String error = respuesta.has("error") ? respuesta.getAsJsonObject("error").get("message").getAsString() : http.body();
            LOGGER.warn("[Hired Hands] API de Anthropic respondió {}: {}", http.statusCode(), error);
            throw new IOException(http.statusCode() == 401 ? "invalid API key"
                    : http.statusCode() == 429 || http.statusCode() >= 500 ? "AI busy, try again in a moment"
                    : "error " + http.statusCode());
        }
        return respuesta;
    }

    private static JsonObject mensaje(String rol, JsonElement contenido) {
        JsonObject o = new JsonObject();
        o.addProperty("role", rol);
        o.add("content", contenido);
        return o;
    }

    private static String texto(JsonArray contenido) {
        StringBuilder s = new StringBuilder();
        for (JsonElement b : contenido) {
            JsonObject o = b.getAsJsonObject();
            if ("text".equals(o.get("type").getAsString())) {
                String t = o.get("text").getAsString().trim();
                if (!t.isEmpty()) s.append(s.isEmpty() ? "" : " ").append(t);
            }
        }
        String texto = s.isEmpty() ? "A la orden." : s.toString();
        return texto.length() > 300 ? texto.substring(0, 300) + "…" : texto;
    }

    private static void decir(Mercenario m, ServerPlayer jugador, Component texto) {
        if (m.isAlive()) m.decirA(jugador, texto);
    }

    // ---------- herramientas ----------

    private static String ejecutar(Mercenario m, ServerPlayer jugador, String herramienta, JsonObject entrada) {
        if (!m.isAlive() || !m.isOwnedBy(jugador)) return "ERROR: ya no trabajas para este jugador.";
        String donde = entrada.has("donde") ? entrada.get("donde").getAsString() : "aqui_conmigo";
        BlockPos lugar = "donde_estoy".equals(donde) ? m.blockPosition() : jugador.blockPosition();
        return switch (herramienta) {
            case "seguir" -> m.ordenSeguir().paraIA();
            case "esperar" -> m.ordenEsperar(lugar).paraIA();
            case "trabajar" -> m.ordenTrabajar(lugar, jugador).paraIA();
            case "buscar_mineral" -> {
                try {
                    var mineral = com.oscarways.hiredhands.trabajo.Mineral.valueOf(entrada.get("mineral").getAsString().toUpperCase());
                    int cantidad = entrada.has("cantidad") ? Math.max(0, entrada.get("cantidad").getAsInt()) : 0;
                    yield m.ordenBuscar(mineral, cantidad, jugador).paraIA();
                } catch (IllegalArgumentException | NullPointerException | UnsupportedOperationException e) {
                    yield "ERROR: mineral desconocido.";
                }
            }
            case "cambiar_oficio" -> {
                try {
                    yield m.ordenOficio(Oficio.valueOf(entrada.get("oficio").getAsString().toUpperCase()), jugador).paraIA();
                } catch (IllegalArgumentException | NullPointerException e) {
                    yield "ERROR: oficio desconocido.";
                }
            }
            default -> "ERROR: herramienta desconocida.";
        };
    }

    private static final String SISTEMA = """
            Eres un mercenario contratado en un servidor de Minecraft de ambientación medieval. Tu patrón te habla por \
            el chat: a veces te da órdenes y a veces solo conversa contigo. Cumple las órdenes con las herramientas \
            disponibles y contesta siempre en personaje y EN EL MISMO IDIOMA en que te escribe tu patrón (si te habla en inglés, en \
            inglés; si en español, en español), en una o dos frases cortas, con un toque de \
            carácter (eres un mercenario curtido, leal a quien paga), sin emojis ni formato. Puedes charlar, opinar y \
            contar cosas de tu vida de mercenario, pero sin inventar hechos del mundo que no estén en tu estado. Si te \
            pide algo que no puedes hacer con tus herramientas, díselo con naturalidad y sugiere qué sí puedes hacer. \
            Si te pregunta algo (cómo estás, qué haces), contesta con los datos de tu estado sin usar herramientas. Para trabajar necesitas la herramienta de tu oficio; si no la tienes, cambia de oficio \
            (buscas la herramienta en tu mochila o en el cofre de tu puesto) o pide que te la den.
            Oficios: mercenario (arma, pelea), lenador (hacha, tala árboles), minero (pico: cava un túnel hacia donde \
            mira el patrón, o hace expediciones si hay una Entrada de mina cerca), granjero (azada, cosecha y siembra), \
            pescador (caña, pesca), ganadero (tijeras: esquila, cría y sacrifica el ganado), cargador (saco: lleva             al cofre del puesto lo que hay en otros cofres), cocinero (cuenco: cocina en hornos y ahumadores).""";

    private static final JsonArray HERRAMIENTAS = GSON.toJsonTree(List.of(
            herramienta("seguir", "Seguir al patrón a todas partes (deja de trabajar o esperar).", Map.of(), List.of()),
            herramienta("esperar", "Quedarse quieto en un sitio, sin trabajar. Un mercenario de oficio monta guardia ahí.",
                    Map.of("donde", donde()), List.of("donde")),
            herramienta("trabajar", "Ponerse a trabajar en su oficio en un sitio (trabaja a 16 bloques a la redonda y deja "
                    + "lo que saca en el cofre más cercano). Si es minero de túnel, cava hacia donde mira el patrón.",
                    Map.of("donde", donde()), List.of("donde")),
            herramienta("buscar_mineral", "Ir a buscar un mineral: se hace minero si hace falta, baja por una escalera "
                    + "(desde donde está el patrón, hacia donde mira) hasta la capa donde más abunda y mina en ramas. "
                    + "cantidad = cuántos traer (0 = sin límite).",
                    Map.of("mineral", Map.of("type", "string",
                                    "enum", List.of("carbon", "cobre", "hierro", "lapis", "oro", "redstone", "diamante")),
                            "cantidad", Map.of("type", "integer", "description", "0 = sin límite")),
                    List.of("mineral", "cantidad")),
            herramienta("cambiar_oficio", "Cambiar de oficio equipándose la herramienta correspondiente, que busca en su "
                    + "mochila o en el cofre de su puesto. Falla si no la encuentra.",
                    Map.of("oficio", Map.of("type", "string",
                            "enum", List.of("mercenario", "lenador", "minero", "granjero", "pescador", "ganadero", "cargador", "cocinero"))),
                    List.of("oficio")))).getAsJsonArray();

    private static Map<String, Object> donde() {
        return Map.of("type", "string", "enum", List.of("aqui_conmigo", "donde_estoy"),
                "description", "aqui_conmigo = donde está el patrón ahora; donde_estoy = donde está el mercenario.");
    }

    private static Map<String, Object> herramienta(String nombre, String descripcion, Map<String, Object> propiedades,
            List<String> requeridos) {
        return Map.of("name", nombre, "description", descripcion, "strict", true,
                "input_schema", Map.of("type", "object", "properties", propiedades, "required", requeridos,
                        "additionalProperties", false));
    }

    // ---------- contexto ----------

    /** Lo que Claude necesita saber: estado del mercenario y qué hay cerca del patrón. */
    private static String contexto(Mercenario m, ServerPlayer jugador) {
        return "Tu estado: " + m.resumenParaIA() + "\n"
                + "Estás a " + Math.round(m.distanceTo(jugador)) + " bloques de tu patrón (" + jugador.getName().getString()
                + "). Es de " + (jugador.level().isDarkOutside() ? "noche" : "día") + ".\n"
                + "Cerca del patrón hay: " + alrededores(jugador) + ".";
    }

    private static String alrededores(ServerPlayer jugador) {
        var level = jugador.level();
        boolean cofre = false, entrada = false, arbol = false, agua = false, cultivo = false, cama = false;
        for (BlockPos p : BlockPos.betweenClosed(jugador.blockPosition().offset(-10, -4, -10),
                jugador.blockPosition().offset(10, 6, 10))) {
            var st = level.getBlockState(p);
            cofre |= st.is(net.neoforged.neoforge.common.Tags.Blocks.CHESTS) || st.is(net.neoforged.neoforge.common.Tags.Blocks.BARRELS);
            entrada |= st.is(HiredHands.ENTRADA_MINA.get());
            arbol |= st.is(net.minecraft.tags.BlockTags.LOGS);
            agua |= st.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
            cultivo |= st.getBlock() instanceof net.minecraft.world.level.block.CropBlock;
            cama |= st.getBlock() instanceof net.minecraft.world.level.block.BedBlock;
        }
        List<String> cosas = new ArrayList<>();
        if (cofre) cosas.add("un cofre");
        if (entrada) cosas.add("una Entrada de mina");
        if (arbol) cosas.add("árboles o madera");
        if (agua) cosas.add("agua");
        if (cultivo) cosas.add("cultivos");
        if (cama) cosas.add("una cama");
        if (jugador.getY() < 40) cosas.add("(está bajo tierra, a altura Y " + jugador.getBlockY() + ")");
        return cosas.isEmpty() ? "nada especial" : String.join(", ", cosas);
    }
}
