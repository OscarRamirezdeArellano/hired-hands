package com.oscarways.hiredhands.entity;

import net.minecraft.network.chat.Component;

/**
 * Resultado de una orden (por chat o de la IA): si se pudo cumplir y qué dice el mercenario, en
 * primera persona y traducible. A la IA se le pasa como texto, con "ERROR:" delante si falló.
 */
public record Orden(boolean ok, Component texto) {
    public static Orden hecho(Component texto) {
        return new Orden(true, texto);
    }

    public static Orden fallo(Component texto) {
        return new Orden(false, texto);
    }

    public String paraIA() {
        return (this.ok ? "" : "ERROR: ") + this.texto.getString();
    }
}
