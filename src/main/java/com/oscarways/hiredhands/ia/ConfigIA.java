package com.oscarways.hiredhands.ia;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuración de la IA (config/hired_hands-common.toml). Es de tipo COMMON: cada lado lee su
 * propio archivo y NO se envía a los jugadores, así que la clave de la API se queda en el servidor.
 */
public final class ConfigIA {
    private ConfigIA() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ACTIVADA;
    public static final ModConfigSpec.ConfigValue<String> CLAVE_API;
    public static final ModConfigSpec.ConfigValue<String> MODELO;
    public static final ModConfigSpec.IntValue LLAMADAS_POR_MINUTO;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Órdenes por chat con Claude: escribe \"Nombre, orden\" y el mercenario te entiende.",
                "Solo cuenta lo que diga el servidor; en los clientes este archivo no se usa.").push("ia");
        ACTIVADA = b.comment("Activa las órdenes por chat.").define("activada", true);
        CLAVE_API = b.comment("Clave de la API de Anthropic (console.anthropic.com). Vacía = usa la variable de entorno ANTHROPIC_API_KEY.")
                .define("clave_api", "");
        MODELO = b.comment("Modelo de Claude. claude-haiku-4-5 es el más barato y rápido y basta para órdenes y charla;",
                "claude-sonnet-5 o claude-opus-5 entienden mejor frases complicadas pero cuestan más.")
                .define("modelo", "claude-haiku-4-5");
        LLAMADAS_POR_MINUTO = b.comment("Máximo de mensajes por minuto que cada jugador puede mandar a la IA.",
                "Las órdenes sencillas (sígueme, quédate, a talar, ¿qué haces?) no llaman a la IA y no cuentan.")
                .defineInRange("llamadas_por_minuto", 6, 1, 60);
        b.pop();
        SPEC = b.build();
    }
}
