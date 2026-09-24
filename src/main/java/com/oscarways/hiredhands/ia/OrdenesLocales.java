package com.oscarways.hiredhands.ia;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.oscarways.hiredhands.Texto;
import com.oscarways.hiredhands.entity.Mercenario;
import com.oscarways.hiredhands.entity.Oficio;
import com.oscarways.hiredhands.entity.Orden;
import com.oscarways.hiredhands.trabajo.Mineral;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * Órdenes sencillas que se entienden sin IA (gratis e instantáneas), en español o en inglés:
 * "sígueme" / "follow me", "quédate aquí" / "stay here", "a talar" / "chop", "busca diamantes" /
 * "find diamonds", "¿qué haces?" / "what are you doing?". Si la frase es larga, lleva una negación o
 * no se reconoce, devuelve null y la orden pasa a Claude.
 */
final class OrdenesLocales {
    private OrdenesLocales() {}

    private static final int MAX_PALABRAS = 6;

    private static final Set<String> NEGACIONES = Set.of("no", "nunca", "deja", "dejes", "sin", "ni", "tampoco",
            "dont", "don", "stop", "never", "not", "without");
    private static final Set<String> SEGUIR = Set.of("sigueme", "sigue", "ven", "vente", "vamos", "vamonos", "acompaname",
            "conmigo", "follow", "come", "lets", "with");
    private static final Set<String> ESPERAR = Set.of("quedate", "espera", "esperame", "espere", "quieto", "alto", "detente",
            "stay", "wait", "halt", "hold");
    private static final Set<String> TRABAJAR = Set.of("trabaja", "trabajar", "trabajo", "chamba", "chambea", "curra", "currar",
            "work", "job");
    private static final Set<String> LENADOR = Set.of("tala", "talar", "lenador", "madera", "lena", "arboles", "troncos",
            "chop", "wood", "lumber", "lumberjack", "trees", "logs");
    private static final Set<String> MINERO = Set.of("mina", "minar", "cava", "cavar", "minero", "pica", "picar", "excava",
            "mine", "dig", "miner");
    private static final Set<String> GRANJERO = Set.of("cosecha", "cosechar", "granja", "granjero", "siembra", "sembrar", "cultiva",
            "farm", "harvest", "farmer", "crops");
    private static final Set<String> PESCADOR = Set.of("pesca", "pescar", "pescador", "peces", "fish", "fishing", "fisher");
    private static final Set<String> GANADERO = Set.of("esquila", "esquilar", "ganado", "ganadero", "animales", "corral",
            "shear", "ranch", "rancher", "livestock", "animals");
    private static final Set<String> CARGADOR = Set.of("carga", "cargar", "cargador", "acarrea", "lleva",
            "haul", "hauler", "carry");
    private static final Set<String> COCINERO = Set.of("cocina", "cocinar", "cocinero", "guisa",
            "cook", "cooking", "chef");
    private static final Set<String> GUARDIA = Set.of("vigila", "guardia", "defiende", "protege", "mercenario",
            "guard", "defend", "protect", "mercenary");
    /** Palabras con las que empieza una pregunta: una pregunta no es una orden ("how much do you carry?"). */
    private static final Set<String> PREGUNTAS = Set.of("how", "what", "why", "where", "when", "who", "which", "do", "does",
            "are", "is", "did", "cuanto", "cuantos", "cuanta", "cuantas", "que", "como", "por", "donde", "cuando", "quien",
            "cual", "tienes", "llevas", "eres", "estas");
    private static final List<String> ESTADO = List.of("que haces", "que estas haciendo", "como estas", "como vas",
            "como va", "que tal", "estado", "informe", "reporte", "novedades",
            "what are you doing", "what you doing", "how are you", "how is it going", "status", "report");

    /** Cuántas frases hay de cada tipo de respuesta (hired_hands.local.<tipo>.<n> en los idiomas). */
    private static final int OK_SEGUIR = 4;
    private static final int OK_ESPERAR = 3;
    private static final int OK_GUARDIA = 3;
    private static final int OK_TRABAJAR = 4;

    /** Respuesta si entendió la orden (y ya la cumplió), o null para pasársela a la IA. */
    static @Nullable Component intentar(Mercenario m, ServerPlayer jugador, String orden) {
        String texto = orden.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (texto.isEmpty()) return null;
        for (String pregunta : ESTADO) {
            if (texto.equals(pregunta) || texto.startsWith(pregunta + " ")) return m.queHaces();
        }
        List<String> palabras = List.of(texto.split(" "));
        // Las preguntas que no son de estado se las dejamos a la IA (sin IA, no hace nada raro).
        if (orden.contains("?") || PREGUNTAS.contains(palabras.get(0))) return null;
        // Negaciones y matices ("no me sigas", "deja de talar", "stop chopping") se los dejamos a la IA.
        if (palabras.size() > MAX_PALABRAS || hay(palabras, NEGACIONES)) return null;

        RandomSource r = m.getRandom();
        BlockPos aqui = jugador.blockPosition();
        // "busca diamantes", "tráeme 10 de hierro", "find 10 diamonds"
        for (String p : palabras) {
            Mineral mineral = Mineral.dePalabra(p);
            if (mineral == null) continue;
            int cantidad = 0;
            for (String q : palabras) {
                if (q.matches("\\d{1,3}")) cantidad = Integer.parseInt(q);
            }
            Orden hecho = m.ordenBuscar(mineral, cantidad, jugador);
            if (!hecho.ok()) return noPuedo(hecho);
            int capa = Math.min(mineral.capa, jugador.getBlockY());
            return cantidad > 0 ? Texto.t("local.a_por_cantidad", cantidad, mineral.nombre(), capa)
                    : Texto.t("local.a_por", mineral.nombre(), capa);
        }
        if (hay(palabras, GUARDIA)) {
            if (m.getOficio() != Oficio.MERCENARIO) {
                Orden cambio = m.ordenOficio(Oficio.MERCENARIO, jugador);
                if (!cambio.ok()) return noPuedo(cambio);
            }
            return respuesta(m.ordenEsperar(aqui), "guardia", OK_GUARDIA, r);
        }
        Oficio pedido = hay(palabras, LENADOR) ? Oficio.LENADOR
                : hay(palabras, MINERO) ? Oficio.MINERO
                : hay(palabras, GRANJERO) ? Oficio.GRANJERO
                : hay(palabras, PESCADOR) ? Oficio.PESCADOR
                : hay(palabras, GANADERO) ? Oficio.GANADERO
                : hay(palabras, CARGADOR) ? Oficio.CARGADOR
                : hay(palabras, COCINERO) ? Oficio.COCINERO
                : hay(palabras, TRABAJAR) ? m.getOficio()
                : null;
        if (pedido != null) {
            if (pedido == Oficio.MERCENARIO) {
                return Texto.t("local.espada");
            }
            if (pedido != m.getOficio()) {
                Orden cambio = m.ordenOficio(pedido, jugador);
                if (!cambio.ok()) return noPuedo(cambio);
            }
            return respuesta(m.ordenTrabajar(aqui, jugador), "trabajar", OK_TRABAJAR, r);
        }
        // Después de los oficios: "vamos a talar" es trabajar, no seguir.
        if (hay(palabras, SEGUIR)) {
            return respuesta(m.ordenSeguir(), "seguir", OK_SEGUIR, r);
        }
        if (hay(palabras, ESPERAR)) {
            return respuesta(m.ordenEsperar(m.blockPosition()), "esperar", OK_ESPERAR, r);
        }
        return null;
    }

    private static boolean hay(List<String> palabras, Set<String> claves) {
        for (String p : palabras) {
            if (claves.contains(p)) return true;
        }
        return false;
    }

    /** Frase al azar de las de ese tipo, o por qué no pudo. */
    private static Component respuesta(Orden resultado, String tipo, int cuantas, RandomSource r) {
        return resultado.ok() ? Texto.t("local." + tipo + "." + r.nextInt(cuantas)) : noPuedo(resultado);
    }

    private static Component noPuedo(Orden resultado) {
        return Texto.t("local.no_puedo", resultado.texto());
    }
}
