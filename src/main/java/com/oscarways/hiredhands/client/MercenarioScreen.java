package com.oscarways.hiredhands.client;

import com.oscarways.hiredhands.Texto;
import com.oscarways.hiredhands.entity.Mercenario;
import com.oscarways.hiredhands.menu.MercenarioMenu;
import com.oscarways.hiredhands.red.OrdenPantalla;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Pantalla de gestión: a la izquierda el mercenario (en 3D, sigue al ratón) y su equipo; a la derecha
 * nivel, experiencia, vida, contrato, qué hace y su grupo; debajo los botones de órdenes, su mochila
 * y tu inventario. El fondo se dibuja con rectángulos al estilo de Minecraft (sin textura propia).
 */
public class MercenarioScreen extends AbstractContainerScreen<MercenarioMenu> {
    private static final int FONDO = 0xFFC6C6C6;
    private static final int CLARO = 0xFFFFFFFF;
    private static final int OSCURO = 0xFF555555;
    private static final int BORDE = 0xFF000000;
    private static final int TEXTO = 0xFF404040;

    private float ratonX;
    private float ratonY;
    private Button botonGrupo;

    public MercenarioScreen(MercenarioMenu menu, Inventory inventario, Component titulo) {
        super(menu, inventario, titulo, MercenarioMenu.ANCHO, MercenarioMenu.ALTO);
        this.inventoryLabelY = MercenarioMenu.Y_INVENTARIO - 11;
    }

    @Override
    protected void init() {
        super.init();
        int y = this.topPos + 92;
        // Cada botón tan ancho como su texto (en español son más largos) y la fila ocupa todo el panel.
        Component[] textos = { Texto.t("pantalla.seguir"), Texto.t("pantalla.esperar"), Texto.t("pantalla.trabajar"),
                Texto.t("pantalla.grupo", 4) };
        int[] anchos = new int[textos.length];
        int total = 0;
        for (int i = 0; i < textos.length; i++) {
            anchos[i] = this.font.width(textos[i]) + 8;
            total += anchos[i];
        }
        int fila = this.imageWidth - 14 - 2 * (textos.length - 1);
        int x = this.leftPos + 7;
        int usado = 0;
        for (int i = 0; i < textos.length; i++) {
            // Reparte el ancho de la fila en proporción al texto; el último se queda con lo que falte.
            int ancho = i < textos.length - 1 ? anchos[i] * fila / total : fila - usado;
            usado += ancho;
            int accion = i;
            Button b = Button.builder(i == 3 ? textoGrupo() : textos[i], boton -> enviar(accion)).bounds(x, y, ancho, 16).build();
            this.addRenderableWidget(b);
            if (i == 3) this.botonGrupo = b;
            x += ancho + 2;
        }
    }

    private void enviar(int accion) {
        Mercenario m = this.menu.getMercenario();
        if (m != null) ClientPacketDistributor.sendToServer(new OrdenPantalla(m.getId(), accion));
    }

    private Component textoGrupo() {
        Mercenario m = this.menu.getMercenario();
        int grupo = m != null ? m.getGrupo() : 0;
        return grupo == 0 ? Texto.t("pantalla.sin_grupo") : Texto.t("pantalla.grupo", grupo);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.ratonX = mouseX;
        this.ratonY = mouseY;
        if (this.botonGrupo != null) this.botonGrupo.setMessage(textoGrupo());
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = this.leftPos;
        int y = this.topPos;
        panel(g, x, y, this.imageWidth, this.imageHeight);
        // Recuadro negro donde se ve al mercenario.
        g.fill(x + 7, y + 17, x + 57, y + 89, BORDE);
        g.fill(x + 8, y + 18, x + 56, y + 88, 0xFF2B2B2B);
        for (Slot slot : this.menu.slots) {
            ranura(g, x + slot.x - 1, y + slot.y - 1);
        }
        Mercenario m = this.menu.getMercenario();
        if (m != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, x + 8, y + 18, x + 56, y + 88, 30, 0.0625F,
                    this.ratonX, this.ratonY, m);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.title, 8, 6, TEXTO, false);
        g.text(this.font, Texto.t("pantalla.mochila"), 8, MercenarioMenu.Y_MOCHILA - 11, TEXTO, false);
        g.text(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, TEXTO, false);

        Mercenario m = this.menu.getMercenario();
        if (m == null) return;
        int x = 100;
        g.text(this.font, m.getOficioSincronizado().nombre(), x, 18, 0xFF1F5E1F, false);
        int nivel = m.getNivel();
        g.text(this.font, Texto.t("pantalla.nivel", nivel), x, 29, TEXTO, false);
        // Barra de experiencia
        int ancho = 68;
        g.fill(x, 39, x + ancho, 42, 0xFF3A3A3A);
        if (nivel >= Mercenario.NIVEL_MAXIMO) {
            g.fill(x, 39, x + ancho, 42, 0xFFE0C040);
        } else {
            int lleno = (int) (ancho * Math.min(1.0, m.getXp() / (double) Mercenario.xpParaSubir(nivel)));
            g.fill(x, 39, x + lleno, 42, 0xFF7FD13B);
        }
        g.text(this.font, Component.literal("❤ " + Math.round(m.getHealth()) + "/" + Math.round(m.getMaxHealth())), x, 46, 0xFFB02020, false);
        long dias = m.diasContrato();
        g.text(this.font, Texto.t("pantalla.contrato", dias > 999 ? "999+" : String.valueOf(dias)), x, 57, TEXTO, false);
        // Qué hace ahora (puede ser largo: se parte en líneas)
        var lineas = this.font.split(m.getEstadoSincronizado(), 70);
        for (int i = 0; i < Math.min(2, lineas.size()); i++) {
            g.text(this.font, lineas.get(i), x, 68 + i * 10, 0xFF2A4A7A, false);
        }
    }

    /** Panel gris con relieve, como los de Minecraft. */
    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BORDE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, CLARO);
        g.fill(x + 3, y + 3, x + w - 1, y + h - 1, OSCURO);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, FONDO);
    }

    /** Ranura hundida de 18x18. */
    private static void ranura(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF373737);
        g.fill(x + 1, y + 1, x + 18, y + 18, CLARO);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }
}
