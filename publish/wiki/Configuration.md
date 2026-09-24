# Configuration

## `config/hired_hands-server.toml`

Created on first server start. Changes apply without a restart.

| Key | Default | Meaning |
|---|---|---|
| `costo_contratacion` | 10 | Emeralds to hire a free mercenary |
| `dias_contrato` | 5 | Days included when hiring or using a contract |
| `dias_por_esmeralda` | 1 | Extra days per emerald |
| `radio_guardia` | 12 | Fighting radius around a guard post |
| `radio_trabajo` | 16 | Work radius around a post (without a work area) |
| `lentitud_trabajo` | 2.0 | Time to break a block compared to a player |
| `largo_tunel` | 64 | Length of the miner's main tunnel |
| `segundos_pesca` | 30 | Average seconds between fish |
| `ganado_maximo` | 10 | Animals per species the rancher keeps |

### `[expedicion]`

| Key | Default | Meaning |
|---|---|---|
| `duracion_segundos` | 300 | Time inside the mine per trip |
| `descanso_segundos` | 20 | Rest after coming back |
| `vida_minima_porcentaje` | 70 | Won't go in with less health |
| `vetas` | 5 | Veins found per trip |
| `probabilidad_herido` | 10 | % chance of coming back hurt |
| `desgaste_pico` | 20 | Pickaxe uses per trip (never breaks it) |

## `config/hired_hands-common.toml` (AI, server only)

```toml
[ia]
    activada = true
    clave_api = "sk-ant-..."
    modelo = "claude-haiku-4-5"
    llamadas_por_minuto = 6
```

The key can also come from the `ANTHROPIC_API_KEY` environment variable. Only the server needs this
file, and it is never sent to players.

## Datapacks

Recipes, spawn weights and spawn biomes are data files under `data/hired_hands/` and can be
overridden with a datapack.
