# Minero

El minero trabaja de dos formas, según haya o no una **Entrada de mina** cerca de su puesto.

## Mina real

![Corte de la mina](/img/04-miner-cutaway.webp)

*Corte: se quitó la roca de encima de la mina para la captura.*

Cava hacia donde **tú** mirabas al ponerlo a trabajar.

1. **Objetivo** (opcional): dale clic con la mena, el mineral crudo o el lingote, o dile
   `Garrick, busca 10 diamantes`. Sin objetivo, mina a la altura de su puesto y se queda con todo.
2. **Escalera** hasta la capa donde más abunda ese mineral:

   | Mineral | Capa |
   |---|---|
   | Diamante, redstone | Y −58 |
   | Oro | Y −16 |
   | Lapislázuli | Y 0 |
   | Hierro | Y 16 |
   | Cobre | Y 48 |
   | Carbón | Y 96 |

   Solo baja: si la capa está por encima del puesto, mina a la altura del puesto.
3. **Minería en ramas**: túnel principal de 64 bloques con ramales de 16 a cada lado cada 3 bloques.
   Saca todas las vetas que ve (primero las del objetivo) y, si busca algo, no pierde tiempo con carbón
   ni cobre.
4. **Obstáculos**: tapa la lava y el agua, pone suelo sobre cuevas, cambia los techos de arena o grava,
   rellena los agujeros que dejan las menas del suelo y vuelve a despejar el camino si se le tapa. Si
   algo no se puede romper (roca madre, un spawner), deja ese ramal.
5. **Piedra**: guarda 64 para tapar cosas y deja el resto.
6. **Cofre de la mina**: si le das un cofre, lo pone al fondo de la escalera.
7. **Objetivo cumplido**: te avisa, deja el mineral en el cofre y para.

## Expediciones

Con una **Entrada de mina** a menos de 4 bloques de su puesto, no cava: entra, desaparece 5 minutos y
vuelve con minerales. No toca el mundo.

- **Lo que trae** depende de la altura de la entrada (por encima de Y 48: carbón, cobre, hierro; de
  Y 0 a 48: además oro, redstone y lapislázuli; bajo Y 0: hierro, oro, redstone, lapislázuli y
  diamante) y de su pico (con pico de piedra no hay diamantes).
- A más nivel, más vetas.
- A veces vuelve herido (nunca muere ahí) y no vuelve a entrar con menos del 70 % de vida.
- Si le das clic mientras está dentro, al salir se queda esperando en su puesto.

![Receta de la entrada de mina](/img/44-recipe-mine-entrance.png)
