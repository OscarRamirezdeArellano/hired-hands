package com.oscarways.hiredhands;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuración del servidor (saves/<mundo>/serverconfig/hired_hands-server.toml).
 * Es de tipo SERVER: la lee solo el servidor y se aplica sin reiniciar.
 */
public final class Config {
    private Config() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue COSTO_CONTRATACION;
    public static final ModConfigSpec.IntValue DIAS_CONTRATO;
    public static final ModConfigSpec.IntValue DIAS_POR_ESMERALDA;
    public static final ModConfigSpec.IntValue RADIO_GUARDIA;
    public static final ModConfigSpec.IntValue RADIO_TRABAJO;
    public static final ModConfigSpec.DoubleValue LENTITUD_TRABAJO;
    public static final ModConfigSpec.IntValue LARGO_TUNEL;
    public static final ModConfigSpec.IntValue EXPEDICION_DURACION;
    public static final ModConfigSpec.IntValue EXPEDICION_DESCANSO;
    public static final ModConfigSpec.IntValue EXPEDICION_VIDA_MINIMA;
    public static final ModConfigSpec.IntValue EXPEDICION_VETAS;
    public static final ModConfigSpec.IntValue EXPEDICION_HERIDO;
    public static final ModConfigSpec.IntValue EXPEDICION_DESGASTE;
    public static final ModConfigSpec.IntValue SEGUNDOS_PESCA;
    public static final ModConfigSpec.IntValue GANADO_MAXIMO;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Contratación. Los días son días de Minecraft (20 minutos reales) y corren aunque nadie esté conectado.")
                .push("contrato");
        COSTO_CONTRATACION = b.comment("Esmeraldas que cobra un mercenario libre por unirse a ti.")
                .defineInRange("costo_contratacion", 10, 1, 64);
        DIAS_CONTRATO = b.comment("Días de servicio que incluye la contratación (y el Contrato de mercenario).")
                .defineInRange("dias_contrato", 5, 1, 365);
        DIAS_POR_ESMERALDA = b.comment("Días que añade cada esmeralda extra que le pagas a tu mercenario.")
                .defineInRange("dias_por_esmeralda", 1, 1, 30);
        b.pop();

        b.push("combate");
        RADIO_GUARDIA = b.comment("En modo guardia, distancia máxima (bloques) a la que se aleja de su puesto para pelear.")
                .defineInRange("radio_guardia", 12, 4, 48);
        b.pop();

        b.comment("Trabajadores: el oficio lo decide la herramienta (hacha, pico, azada, caña).",
                "Trabajan de guardia, alrededor de su puesto, y dejan lo que sacan en el cofre más cercano al puesto.")
                .push("trabajo");
        RADIO_TRABAJO = b.comment("Distancia (bloques) alrededor del puesto donde buscan árboles, cultivos o agua.")
                .defineInRange("radio_trabajo", 16, 4, 48);
        LENTITUD_TRABAJO = b.comment("Tiempo para romper un bloque comparado con un jugador con la misma herramienta (2 = el doble).")
                .defineInRange("lentitud_trabajo", 2.0, 0.5, 20.0);
        LARGO_TUNEL = b.comment("Largo máximo del túnel que cava un minero antes de pararse.")
                .defineInRange("largo_tunel", 64, 4, 512);
        SEGUNDOS_PESCA = b.comment("Tiempo medio (segundos) entre cada pez.")
                .defineInRange("segundos_pesca", 30, 5, 600);
        GANADO_MAXIMO = b.comment("Ganadero: animales de cada especie que mantiene en su zona (cría si hay menos, sacrifica si hay más).")
                .defineInRange("ganado_maximo", 10, 2, 64);
        b.pop();

        b.comment("Expediciones del minero a una Entrada de mina (el minero no toca el mundo: desaparece y vuelve con minerales).",
                "Los tiempos son reales y cuentan aunque nadie esté cerca.").push("expedicion");
        EXPEDICION_DURACION = b.comment("Segundos que pasa dentro de la mina en cada viaje.")
                .defineInRange("duracion_segundos", 300, 10, 7200);
        EXPEDICION_DESCANSO = b.comment("Segundos que descansa desde que vuelve (incluye el rato que tarda en dejar lo que trajo en el cofre).")
                .defineInRange("descanso_segundos", 20, 0, 3600);
        EXPEDICION_VIDA_MINIMA = b.comment("No entra si su vida está por debajo de este porcentaje; espera a curarse.")
                .defineInRange("vida_minima_porcentaje", 70, 0, 100);
        EXPEDICION_VETAS = b.comment("Vetas de mineral que encuentra en cada viaje (cada veta son de 1 a 6 bloques).",
                "Es lo que más cambia cuánto trae: el doble de vetas, el doble de minerales.")
                .defineInRange("vetas", 5, 1, 50);
        EXPEDICION_HERIDO = b.comment("Probabilidad (%) de volver herido (pierde de 4 a 10 de vida, nunca muere).")
                .defineInRange("probabilidad_herido", 10, 0, 100);
        EXPEDICION_DESGASTE = b.comment("Usos del pico que gasta en cada viaje (un pico de hierro tiene 250).")
                .defineInRange("desgaste_pico", 20, 0, 500);
        b.pop();

        SPEC = b.build();
    }
}
