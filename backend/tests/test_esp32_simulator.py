"""El simulador (tools/esp32_simulator) debe producir paquetes que cumplan el contrato."""

import random
import sys
from datetime import UTC, datetime, timedelta
from pathlib import Path

import pytest

from app.schemas import TelemetryIn

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tools" / "esp32_simulator"))
import esp32_simulator as sim  # noqa: E402

START = datetime(2026, 9, 11, 18, 30, tzinfo=UTC)


def run(scenario: str, samples: int, **kwargs) -> list[dict]:
    rng = random.Random(1)
    device = sim.VirtualDevice("esp32-main", 12.2, rng)
    return [
        device.packet(START + timedelta(seconds=5 * tick), sim.scenario_sample(scenario, tick, rng), **kwargs)
        for tick in range(samples)
    ]


@pytest.mark.parametrize("scenario", sim.SCENARIOS)
def test_every_scenario_matches_the_contract(scenario):
    for body in run(scenario, 40):
        TelemetryIn.model_validate(body)


def test_blocks_can_be_disabled():
    [body] = run("normal", 1, electricity=False)
    assert "electricity" not in body
    assert TelemetryIn.model_validate(body).water is not None


def test_device_step_goes_base_plus5_base_plus12_base_plus25():
    offsets = [sim.step_offset(tick, 3) for tick in range(18)]
    assert offsets == [0, 0, 0, 5, 5, 5, 0, 0, 0, 12, 12, 12, 0, 0, 0, 25, 25, 25]


def test_scenarios_have_their_expected_shape():
    def avg(values):
        return sum(values) / len(values)

    power = {s: avg([p["electricity"]["power"] for p in run(s, 60)]) for s in sim.SCENARIOS}
    flow = {s: [p["water"]["flow_liters_per_minute"] for p in run(s, 60)] for s in sim.SCENARIOS}

    assert power["high_consumption"] > 3 * power["normal"]
    assert all(q > 1 for q in flow["water_flow"])
    assert all(0 < q < 0.5 for q in flow["water_leak"])  # goteo continuo
    assert any(q > 0 for q in flow["normal"]) and any(q == 0 for q in flow["normal"])


def test_counters_are_monotonic_and_integrate_the_previous_rate():
    packets = run("high_consumption", 10)
    energy = [p["electricity"]["energy_kwh"] for p in packets]
    assert energy[0] == 0
    assert energy == sorted(energy)
    expected = sum(p["electricity"]["power"] for p in packets[:-1]) * 5 / 3_600_000
    assert energy[-1] == pytest.approx(expected, abs=1e-6)
