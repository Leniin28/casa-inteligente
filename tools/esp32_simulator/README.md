# Simulador de ESP32

Hace de ESP32 desde la laptop: envía telemetría **real por HTTP** a `POST /api/telemetry` con el
contrato de [docs/TELEMETRY-CONTRACT.md](../../docs/TELEMETRY-CONTRACT.md). Solo usa la librería
estándar de Python (3.11+); no hay que instalar nada.

## Uso

El backend debe tener `SMARTHOME_DEVICE_API_KEY` configurada (y `SMARTHOME_DATA_MODE=sensors` para ver
los datos en la app). Ver [docs/ESP32-INTEGRATION.md](../../docs/ESP32-INTEGRATION.md).

```powershell
$env:SMARTHOME_DEVICE_API_KEY = "<la misma clave que en backend/.env>"
python tools/esp32_simulator/esp32_simulator.py --scenario normal
python tools/esp32_simulator/esp32_simulator.py --scenario device_step --interval 2 --count 60
python tools/esp32_simulator/esp32_simulator.py --scenario water_leak --no-electricity
python tools/esp32_simulator/esp32_simulator.py --scenario high_consumption --dry-run --count 3
```

Salida: una línea por paquete con el código HTTP. Termina con código 1 si algún envío falló.

## Escenarios

| Escenario | Electricidad | Agua |
|---|---|---|
| `normal` | ~7 W estable | grifo abierto a ratos (~1.1 L/min) |
| `high_consumption` | ~38 W | sin flujo |
| `water_flow` | ~6 W | flujo continuo ~1.6 L/min |
| `water_leak` | ~4 W | goteo continuo ~0.12 L/min (fuga) |
| `device_step` | base 4 W → +5 W → base → +12 W → base → +25 W → base (repite) | sin flujo |

`device_step` genera escalones limpios para el futuro trabajo de NILM; `--step-samples` fija cuántos
paquetes dura cada escalón.

## Opciones

| Opción | Por defecto | |
|---|---|---|
| `--url` | `http://127.0.0.1:8000` (env `ESP32_SIM_URL`) | URL base del backend |
| `--device-key` | env `SMARTHOME_DEVICE_API_KEY` | mejor por variable de entorno que por línea de comandos |
| `--device-id` | `esp32-main` | |
| `--interval` | `5` | segundos entre paquetes |
| `--count` | `0` (sin fin, Ctrl+C para parar) | |
| `--no-electricity` / `--no-water` | — | omitir un bloque |
| `--nominal-voltage` | `12.2` | p. ej. `230` para simular AC |
| `--step-samples` | `6` | paquetes por escalón en `device_step` |
| `--seed` | aleatoria | reproducibilidad |
| `--dry-run` | — | imprime el JSON sin enviarlo (no necesita clave) |

El simulador mantiene contadores acumulados (`energy_kwh`, `total_liters`) como un medidor real;
al reiniciarlo empiezan de 0 y el backend lo trata como un reinicio del ESP32.
