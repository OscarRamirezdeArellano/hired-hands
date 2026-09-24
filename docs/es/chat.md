# Chat e IA

![Chat](/img/12-chat.webp)

## Saludos

Cuando te acercas (a 6 bloques y a la vista) tu contratado te saluda en el chat. Los mercenarios
libres saludan a cualquiera y ofrecen sus servicios con su precio. Como mucho un saludo cada 5 minutos
por jugador. No usa IA.

## Órdenes por chat

Empieza el mensaje con su **primer nombre** seguido de coma o dos puntos: `Wren, sígueme`. Funciona
con tus contratados a menos de 64 bloques. Todos ven tu mensaje como siempre; la respuesta solo la ves
tú.

Las órdenes sencillas funcionan **sin IA**, al instante:

| Dices | Hace |
|---|---|
| sígueme, ven, vamos, acompáñame | Te sigue |
| quédate, espera, quieto, alto | Espera donde está |
| vigila, guardia, defiende, protege | Monta guardia donde estás tú (vuelve a mercenario) |
| a talar, a minar, a cosechar, a pescar, esquila, carga, cocina | Cambia a ese oficio (busca la herramienta en su mochila o en el cofre) y trabaja donde estás tú |
| busca diamantes, tráeme 10 de hierro | Se hace minero y va por ese mineral |
| trabaja, a trabajar | Trabaja en su oficio donde estás tú |
| ¿qué haces?, ¿cómo vas?, informe | Te cuenta qué hace, qué lleva y cuántos días de contrato le quedan |

Las mismas órdenes funcionan en inglés (`follow me`, `stay`, `chop`, `find diamonds`,
`what are you doing?`).

Órdenes a varios: `todos, <orden>` o `grupo 2, <orden>` (solo órdenes sencillas).

## IA opcional

Todo lo demás (frases largas, con "no", preguntas, charla) puede ir a **Claude**, que entiende la
orden, la cumple con las mismas acciones que las órdenes sencillas y contesta en personaje. Recuerda
los últimos 4 intercambios durante 10 minutos y responde en el idioma en que le hablas.

::: info Desactivada por defecto
La IA solo funciona si el dueño del servidor pone una clave de la API de Anthropic (ver
[Configuración](./configuration)). La clave se queda en el servidor. El modelo por defecto, Claude
Haiku 4.5, es el más barato, y cada jugador puede mandar hasta 6 mensajes por minuto a la IA.
:::
