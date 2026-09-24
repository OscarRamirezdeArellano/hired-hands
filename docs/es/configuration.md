# Configuración

## `config/hired_hands-server.toml`

Se crea al arrancar el servidor. Los cambios se aplican sin reiniciar.

| Clave | Por defecto | Qué es |
|---|---|---|
| `costo_contratacion` | 10 | Esmeraldas para contratar a un mercenario libre |
| `dias_contrato` | 5 | Días que incluye la contratación o el contrato |
| `dias_por_esmeralda` | 1 | Días extra por esmeralda |
| `radio_guardia` | 12 | Radio de combate alrededor de un puesto de guardia |
| `radio_trabajo` | 16 | Radio de trabajo alrededor del puesto (sin zona de trabajo) |
| `lentitud_trabajo` | 2.0 | Tiempo para romper un bloque comparado con un jugador |
| `largo_tunel` | 64 | Largo del túnel principal del minero |
| `segundos_pesca` | 30 | Segundos promedio entre peces |
| `ganado_maximo` | 10 | Animales por especie que conserva el ganadero |

### `[expedicion]`

| Clave | Por defecto | Qué es |
|---|---|---|
| `duracion_segundos` | 300 | Tiempo dentro de la mina en cada viaje |
| `descanso_segundos` | 20 | Descanso al volver |
| `vida_minima_porcentaje` | 70 | No entra con menos vida |
| `vetas` | 5 | Vetas que encuentra por viaje |
| `probabilidad_herido` | 10 | % de volver herido |
| `desgaste_pico` | 20 | Usos de pico por viaje (nunca lo rompe) |

## IA: `config/hired_hands-common.toml`

Solo lo necesita el **servidor**, y nunca se envía a los jugadores.

```toml
[ia]
    activada = true
    clave_api = "sk-ant-..."
    modelo = "claude-haiku-4-5"
    llamadas_por_minuto = 6
```

La clave también puede venir de la variable de entorno `ANTHROPIC_API_KEY`. Sin clave, los saludos y
las órdenes sencillas funcionan igual.

## Datapacks

Las recetas, la frecuencia de aparición y los biomas son archivos de datos en `data/hired_hands/` y se
pueden cambiar con un datapack.
