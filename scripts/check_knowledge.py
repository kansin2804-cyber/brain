#!/usr/bin/env python3
"""JU brain knowledge quality checker.

Checks:
1. Manifest paths exist and agent graph is consistent
2. Banned brand/visual terms (with allow-list for rule docs)
3. Required markdown section headers by agent level
4. Template JSON files validate against schemas (Draft 2020-12 subset)

Exit codes:
  0 = pass (warnings allowed)
  1 = errors found
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
MANIFEST_PATH = ROOT / "knowledge" / "manifest.json"
BANNED_PATH = ROOT / "scripts" / "banned_terms.json"
SCAN_GLOBS = [
    "knowledge/**/*.md",
    "templates/**/*.json",
    "README.md",
]


class Findings:
    def __init__(self) -> None:
        self.errors: list[str] = []
        self.warnings: list[str] = []

    def error(self, msg: str) -> None:
        self.errors.append(msg)

    def warn(self, msg: str) -> None:
        self.warnings.append(msg)


def load_json(path: Path) -> Any:
    with path.open(encoding="utf-8") as f:
        return json.load(f)


def check_manifest(findings: Findings, manifest: dict[str, Any]) -> dict[str, Any]:
    agents = {a["id"]: a for a in manifest.get("agents", [])}
    if not agents:
        findings.error("manifest.agents is empty")
        return agents

    for entry in manifest.get("shared", []):
        path = ROOT / entry["path"]
        if not path.is_file():
            findings.error(f"shared missing file: {entry['path']}")

    for agent_id, agent in agents.items():
        path = ROOT / agent["path"]
        if not path.is_file():
            findings.error(f"agent {agent_id} missing file: {agent['path']}")

        parent = agent.get("reports_to")
        if parent and parent not in agents:
            findings.error(f"agent {agent_id} reports_to unknown id: {parent}")

        for child in agent.get("children", []) or []:
            if child not in agents:
                findings.error(f"agent {agent_id} children unknown id: {child}")
            elif agents[child].get("reports_to") not in (agent_id, None):
                # allow guide-like soft links; only warn on hard mismatch
                if agents[child].get("reports_to") != agent_id:
                    findings.warn(
                        f"agent {child} reports_to={agents[child].get('reports_to')} "
                        f"but listed under {agent_id}.children"
                    )

        for guide in agent.get("guides", []) or []:
            if guide not in agents:
                findings.error(f"agent {agent_id} guides unknown id: {guide}")

    if "ju_orchestrator" not in agents:
        findings.error("manifest missing ju_orchestrator")

    for key, rel in (manifest.get("templates") or {}).items():
        if not (ROOT / rel).is_file():
            findings.error(f"template '{key}' missing: {rel}")
    for key, rel in (manifest.get("schemas") or {}).items():
        if not (ROOT / rel).is_file():
            findings.error(f"schema '{key}' missing: {rel}")

    return agents


def iter_scan_files() -> list[Path]:
    files: set[Path] = set()
    for pattern in SCAN_GLOBS:
        files.update(ROOT.glob(pattern))
    return sorted(p for p in files if p.is_file())


def path_allowed(rel: str, allow_list: list[str]) -> bool:
    rel_norm = rel.replace("\\", "/")
    for allowed in allow_list:
        if rel_norm == allowed or rel_norm.endswith("/" + allowed):
            return True
    return False


def check_banned_terms(findings: Findings) -> None:
    cfg = load_json(BANNED_PATH)
    terms = cfg.get("terms", [])
    for path in iter_scan_files():
        rel = path.relative_to(ROOT).as_posix()
        text = path.read_text(encoding="utf-8")
        for term in terms:
            pattern = term["pattern"]
            severity = term.get("severity", "error")
            allow = term.get("allow_in_paths", [])
            if path_allowed(rel, allow):
                continue
            for i, line in enumerate(text.splitlines(), start=1):
                if re.search(pattern, line):
                    msg = (
                        f"banned term [{term['id']}] {rel}:{i}: "
                        f"{line.strip()[:120]}"
                    )
                    if severity == "warning":
                        findings.warn(msg)
                    else:
                        findings.error(msg)


def check_required_sections(
    findings: Findings, manifest: dict[str, Any], agents: dict[str, Any]
) -> None:
    required_map = manifest.get("required_sections_by_level") or {}
    for agent_id, agent in agents.items():
        level = agent.get("level", "")
        needed = required_map.get(level) or []
        if not needed:
            continue
        path = ROOT / agent["path"]
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8")
        headers = set(re.findall(r"^##\s+(.+)$", text, flags=re.M))
        for section_group in needed:
            # Each entry is a string or a list of acceptable header aliases.
            aliases = (
                section_group
                if isinstance(section_group, list)
                else [section_group]
            )
            if not any(alias in headers for alias in aliases):
                findings.error(
                    f"agent {agent_id} missing required section "
                    f"(one of {aliases}) in {agent['path']}"
                )


def type_ok(value: Any, expected: Any) -> bool:
    if isinstance(expected, list):
        return any(type_ok(value, opt) for opt in expected)
    mapping = {
        "object": dict,
        "array": list,
        "string": str,
        "boolean": bool,
        "number": (int, float),
        "null": type(None),
    }
    py = mapping.get(expected)
    if py is None:
        return True
    return isinstance(value, py)


def validate_schema(instance: Any, schema: dict[str, Any], path: str = "$") -> list[str]:
    """Minimal Draft 2020-12 validator for our template schemas."""
    errors: list[str] = []

    if "const" in schema and instance != schema["const"]:
        errors.append(f"{path}: expected const {schema['const']!r}, got {instance!r}")

    if "enum" in schema and instance not in schema["enum"]:
        errors.append(f"{path}: value {instance!r} not in enum {schema['enum']}")

    if "type" in schema and not type_ok(instance, schema["type"]):
        errors.append(f"{path}: type mismatch, expected {schema['type']}")
        return errors

    if schema.get("type") == "string" or (
        isinstance(schema.get("type"), list) and "string" in schema["type"]
    ):
        if isinstance(instance, str) and "minLength" in schema:
            if len(instance) < schema["minLength"]:
                errors.append(f"{path}: string shorter than minLength")

    if isinstance(instance, dict) and schema.get("type") in ("object", None):
        required = schema.get("required") or []
        for key in required:
            if key not in instance:
                errors.append(f"{path}: missing required property '{key}'")
        props = schema.get("properties") or {}
        for key, value in instance.items():
            if key in props:
                errors.extend(validate_schema(value, props[key], f"{path}.{key}"))

    if isinstance(instance, list) and schema.get("type") == "array":
        if "minItems" in schema and len(instance) < schema["minItems"]:
            errors.append(f"{path}: fewer than minItems ({schema['minItems']})")
        if "maxItems" in schema and len(instance) > schema["maxItems"]:
            errors.append(f"{path}: more than maxItems ({schema['maxItems']})")
        item_schema = schema.get("items")
        if isinstance(item_schema, dict):
            for i, item in enumerate(instance):
                errors.extend(validate_schema(item, item_schema, f"{path}[{i}]"))

    return errors


def check_templates(findings: Findings, manifest: dict[str, Any]) -> None:
    templates = manifest.get("templates") or {}
    schemas = manifest.get("schemas") or {}
    for key, template_rel in templates.items():
        schema_rel = schemas.get(key)
        if not schema_rel:
            findings.warn(f"template '{key}' has no schema mapping")
            continue
        template_path = ROOT / template_rel
        schema_path = ROOT / schema_rel
        if not template_path.is_file() or not schema_path.is_file():
            continue
        instance = load_json(template_path)
        schema = load_json(schema_path)
        for err in validate_schema(instance, schema):
            findings.error(f"template {template_rel} schema: {err}")


def check_orphan_knowledge_files(
    findings: Findings, manifest: dict[str, Any], agents: dict[str, Any]
) -> None:
    known = {entry["path"] for entry in manifest.get("shared", [])}
    known.update(a["path"] for a in agents.values())
    for path in sorted((ROOT / "knowledge").rglob("*.md")):
        rel = path.relative_to(ROOT).as_posix()
        if rel == "knowledge/manifest.json":
            continue
        if rel not in known:
            findings.warn(f"knowledge file not listed in manifest: {rel}")


def main() -> int:
    findings = Findings()
    if not MANIFEST_PATH.is_file():
        print(f"ERROR: missing {MANIFEST_PATH}", file=sys.stderr)
        return 1
    if not BANNED_PATH.is_file():
        print(f"ERROR: missing {BANNED_PATH}", file=sys.stderr)
        return 1

    manifest = load_json(MANIFEST_PATH)
    agents = check_manifest(findings, manifest)
    check_banned_terms(findings)
    check_required_sections(findings, manifest, agents)
    check_templates(findings, manifest)
    check_orphan_knowledge_files(findings, manifest, agents)

    for w in findings.warnings:
        print(f"WARN  {w}")
    for e in findings.errors:
        print(f"ERROR {e}")

    print(
        f"\nSummary: {len(findings.errors)} error(s), "
        f"{len(findings.warnings)} warning(s)"
    )
    return 1 if findings.errors else 0


if __name__ == "__main__":
    sys.exit(main())
