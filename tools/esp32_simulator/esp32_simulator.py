"""Simulador de ESP32: envía telemetría normalizada por HTTP al backend (POST /api/telemetry).

Solo usa la librería estándar de Python. Ejemplos:

    set SMARTHOME_DEVICE_API_KEY=<la misma clave que en backend/.env>
    python tools/esp32_simulator/esp32_simulator.py --scenario normal
    python tools/esp32_simulator/esp32_simulator.py --scenario device_step --interval 2 --count 60
    python tools/esp32_simulator/esp32_simulator.py --scenario water_leak --no-electricity --dry-run

Ver tools/esp32_simulator/README.md y docs/TELEMETRY-CONTRACT.md.
"""

import argparse
import json
import os
import random
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import UTC, datetime

SCENARIOS = ("normal", "high_consumption", "water_flow", "water_leak", "device_step")

BASE_WATTS = 4.0
# device_step: base → +5 W → base → +12 W → base → +25 W → base ... (huellas para NILM futuro)
STEP_WATTS = (5.0, 12.0, 25.0)
# normal: el grifo se abre 1 de cada 6 bloques de 10 muestras
NORMAL_TAP_BLOCK = 10
NORMAL_TAP_EVERY = 6


@dataclass(frozen=True)
class Sample:
    power: float  # W
    flow: float  # L/min


def step_offset(tick: int, step_samples: int) -> float:
    """Potencia añadida en `tick` para device_step: alterna base y cada escalón."""
    phase = (tick // step_samples) % (2 * len(STEP_WATTS))
    return STEP_WATTS[phase // 2] if phase % 2 == 1 else 0.0


def scenario_sample(scenario: str, tick: int, rng: random.Random, step_samples: int = 6) -> Sample:
    def jitter(scale: float) -> float:
        return rng.uniform(-scale, scale)

    if scenario == "normal":
        power = BASE_WATTS + 3.0 + jitter(0.4)
        tap_open = (tick // NORMAL_TAP_BLOCK) % NORMAL_TAP_EVERY == 0
        flow = 1.1 + jitter(0.1) if tap_open else 0.0
    elif scenario == "high_consumption":
        power, flow = 38.0 + jitter(3.0), 0.0
    elif scenario == "water_flow":
        power, flow = BASE_WATTS + 2.0 + jitter(0.3), 1.6 + jitter(0.2)
    elif scenario == "water_leak":
        power, flow = BASE_WATTS + jitter(0.2), 0.12 + jitter(0.01)  # goteo continuo
    elif scenario == "device_step":
        power, flow = BASE_WATTS + step_offset(tick, step_samples) + jitter(0.15), 0.0
    else:
        raise ValueError(f"escenario desconocido: {scenario}")
    return Sample(max(power, 0.0), max(flow, 0.0))


class VirtualDevice:
    """Estado del ESP32 simulado: contadores acumulados como los de un medidor real."""

    def __init__(self, device_id: str, nominal_voltage: float, rng: random.Random):
        self.device_id = device_id
        self.nominal_voltage = nominal_voltage
        self.rng = rng
        self.energy_kwh = 0.0
        self.total_liters = 0.0
        self._last: tuple[datetime, Sample] | None = None

    def packet(self, at: datetime, sample: Sample, electricity: bool = True, water: bool = True) -> dict:
        if self._last is not None:  # lo consumido desde el paquete anterior, al ritmo anterior
            last_at, last_sample = self._last
            seconds = max((at - last_at).total_seconds(), 0.0)
            self.energy_kwh += last_sample.power * seconds / 3_600_000
            self.total_liters += last_sample.flow * seconds / 60
        self._last = (at, sample)

        body: dict = {"device_id": self.device_id, "timestamp": iso_utc(at)}
        if electricity:
            voltage = self.nominal_voltage * (1 - 0.0003 * sample.power) + self.rng.uniform(-1, 1) * self.nominal_voltage * 0.002
            body["electricity"] = {
                "voltage": round(voltage, 3),
                "current": round(sample.power / voltage, 4),
                "power": round(sample.power, 3),
                "energy_kwh": round(self.energy_kwh, 6),
            }
        if water:
            body["water"] = {
                "flow_liters_per_minute": round(sample.flow, 3),
                "total_liters": round(self.total_liters, 4),
            }
        return body


def iso_utc(at: datetime) -> str:
    return at.astimezone(UTC).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def send(url: str, device_key: str, body: dict, timeout: float = 5.0) -> tuple[int, str]:
    request = urllib.request.Request(
        url,
        data=json.dumps(body).encode(),
        method="POST",
        headers={"Content-Type": "application/json", "X-Device-Key": device_key},
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, response.read().decode()
    except urllib.error.HTTPError as exc:
        return exc.code, exc.read().decode()


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Envía telemetría simulada de un ESP32 al backend.")
    parser.add_argument("--scenario", choices=SCENARIOS, default="normal")
    parser.add_argument("--url", default=os.environ.get("ESP32_SIM_URL", "http://127.0.0.1:8000"),
                        help="URL base del backend (env ESP32_SIM_URL)")
    parser.add_argument("--device-key", default=os.environ.get("SMARTHOME_DEVICE_API_KEY"),
                        help="clave de dispositivo; mejor por la variable SMARTHOME_DEVICE_API_KEY")
    parser.add_argument("--device-id", default="esp32-main")
    parser.add_argument("--interval", type=float, default=5.0, help="segundos entre paquetes")
    parser.add_argument("--count", type=int, default=0, help="número de paquetes (0 = sin fin)")
    parser.add_argument("--no-electricity", action="store_true", help="no enviar el bloque electricity")
    parser.add_argument("--no-water", action="store_true", help="no enviar el bloque water")
    parser.add_argument("--nominal-voltage", type=float, default=12.2, help="tensión nominal (V)")
    parser.add_argument("--step-samples", type=int, default=6, help="muestras por escalón en device_step")
    parser.add_argument("--seed", type=int, default=None)
    parser.add_argument("--dry-run", action="store_true", help="imprime los paquetes sin enviarlos")
    args = parser.parse_args(argv)
    if args.no_electricity and args.no_water:
        parser.error("no se puede desactivar electricity y water a la vez")
    if args.interval <= 0 or args.step_samples <= 0 or args.count < 0:
        parser.error("--interval y --step-samples deben ser > 0 y --count >= 0")
    if not args.dry_run and not args.device_key:
        parser.error("falta la clave: define SMARTHOME_DEVICE_API_KEY o usa --device-key")
    return args


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    rng = random.Random(args.seed)
    device = VirtualDevice(args.device_id, args.nominal_voltage, rng)
    endpoint = args.url.rstrip("/") + "/api/telemetry"
    print(f"Simulando {args.device_id} | escenario {args.scenario} | cada {args.interval}s -> {endpoint}")

    failures = 0
    tick = 0
    try:
        while args.count == 0 or tick < args.count:
            sample = scenario_sample(args.scenario, tick, rng, args.step_samples)
            body = device.packet(datetime.now(UTC), sample, not args.no_electricity, not args.no_water)
            if args.dry_run:
                print(json.dumps(body))
            else:
                try:
                    code, text = send(endpoint, args.device_key, body)
                except (urllib.error.URLError, TimeoutError, ConnectionError) as exc:
                    code, text = 0, str(exc)
                ok = code in (200, 201)
                failures += 0 if ok else 1
                summary = f"P={sample.power:5.1f} W  Q={sample.flow:4.2f} L/min"
                print(f"[{tick:4d}] {code or 'ERR'} {summary}" + ("" if ok else f"  {text}"), flush=True)
            tick += 1
            if args.count == 0 or tick < args.count:
                time.sleep(args.interval)
    except KeyboardInterrupt:
        print("\nDetenido.")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
