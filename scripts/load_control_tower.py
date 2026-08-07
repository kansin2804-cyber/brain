#!/usr/bin/env python3
"""Load JU brain knowledge in Control Tower order.

Order (stable, de-duplicated):
  1. knowledge/shared/*  (manifest.shared order)
  2. Requested agent file
  3. Ancestors via reports_to (chief/hq) — on by default
  4. children (+ guides for hq/chief) — DFS / manifest order
  5. Optional few-shot examples by artifact kind

Stdlib only. Exit != 0 if any resolved path is missing.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any, Iterable, Sequence

ROOT = Path(__file__).resolve().parents[1]
MANIFEST_PATH = ROOT / "knowledge" / "manifest.json"

EXAMPLE_GROUPS = (
    "handoff",
    "synthesis",
    "daily_report",
    "shorts_plan",
    "brand_guard",
)

EXAMPLE_DIR_ALIASES = {
    "shorts_plan": "shorts",
}


class LoadError(Exception):
    pass


def load_manifest(path: Path = MANIFEST_PATH) -> dict[str, Any]:
    if not path.is_file():
        raise LoadError(f"manifest not found: {path}")
    with path.open(encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, dict):
        raise LoadError("manifest root must be an object")
    return data


def agent_index(manifest: dict[str, Any]) -> dict[str, dict[str, Any]]:
    agents = {a["id"]: a for a in manifest.get("agents", []) if isinstance(a, dict) and "id" in a}
    if not agents:
        raise LoadError("manifest.agents is empty")
    return agents


def unique_extend(ordered: list[str], paths: Iterable[str]) -> None:
    seen = set(ordered)
    for p in paths:
        if p and p not in seen:
            ordered.append(p)
            seen.add(p)


def shared_paths(manifest: dict[str, Any]) -> list[str]:
    out: list[str] = []
    for entry in manifest.get("shared") or []:
        if isinstance(entry, dict) and entry.get("path"):
            out.append(entry["path"])
        elif isinstance(entry, str):
            out.append(entry)
    return out


def ancestors(agent_id: str, agents: dict[str, dict[str, Any]]) -> list[str]:
    """Return reports_to chain from immediate parent up to root (bottom→top)."""
    chain: list[str] = []
    seen: set[str] = set()
    cur = agents[agent_id].get("reports_to")
    while cur:
        if cur in seen:
            raise LoadError(f"reports_to cycle involving {cur}")
        if cur not in agents:
            raise LoadError(f"unknown reports_to parent: {cur}")
        seen.add(cur)
        chain.append(cur)
        cur = agents[cur].get("reports_to")
    return chain


def walk_descendants(
    agent_id: str,
    agents: dict[str, dict[str, Any]],
    *,
    include_guides: bool,
) -> list[str]:
    """DFS over children (then guides if requested), manifest list order."""
    out: list[str] = []
    seen: set[str] = set()

    def visit(aid: str, allow_guides: bool) -> None:
        node = agents[aid]
        for child in node.get("children") or []:
            if child not in agents:
                raise LoadError(f"{aid} children unknown id: {child}")
            if child in seen:
                continue
            seen.add(child)
            out.append(child)
            # specialists recurse into their own children; guides recurse if listed
            visit(child, allow_guides=agents[child].get("level") in ("hq", "chief"))
        if allow_guides:
            for guide in node.get("guides") or []:
                if guide not in agents:
                    raise LoadError(f"{aid} guides unknown id: {guide}")
                if guide in seen:
                    continue
                seen.add(guide)
                out.append(guide)

    visit(agent_id, allow_guides=include_guides)
    return out


def resolve_agent_set(
    *,
    agent_id: str | None,
    domain: str | None,
    agents: dict[str, dict[str, Any]],
    include_ancestors: bool,
    include_children: bool | None,
    include_guides: bool | None,
) -> tuple[str | None, list[str]]:
    """Return (primary_agent_id, ordered agent ids excluding shared)."""
    if domain and not agent_id:
        chiefs = [
            aid
            for aid, a in agents.items()
            if a.get("domain") == domain and a.get("level") == "chief"
        ]
        if not chiefs:
            # domain may be shorts — use specialist root under marketing
            specialists = [
                aid
                for aid, a in agents.items()
                if a.get("domain") == domain and a.get("level") == "specialist" and not a.get("reports_to")
            ]
            # prefer domain agents whose parent is a chief of another domain or orchestrator
            domain_agents = [aid for aid, a in agents.items() if a.get("domain") == domain]
            if not domain_agents:
                raise LoadError(f"no agents for domain: {domain}")
            # pick chief-like: prefer ju_chief_* else first specialist that has children
            preferred = [a for a in domain_agents if a.startswith("ju_chief_")]
            if not preferred:
                preferred = [
                    a
                    for a in domain_agents
                    if agents[a].get("level") == "specialist" and agents[a].get("children")
                ] or domain_agents
            agent_id = preferred[0]
        else:
            agent_id = chiefs[0]

    if not agent_id:
        raise LoadError("--agent is required (or provide --domain)")

    if agent_id not in agents:
        raise LoadError(f"unknown agent id: {agent_id}")

    level = agents[agent_id].get("level") or "specialist"
    if include_children is None:
        # hq/chief: always expand; specialist: expand when children exist
        if level in ("hq", "chief"):
            include_children = True
        else:
            include_children = bool(agents[agent_id].get("children"))
    if include_guides is None:
        include_guides = level in ("hq", "chief")

    # Goal order after shared: HQ/chief (ancestors, root-first) → self → children/guides
    ordered: list[str] = []
    if include_ancestors:
        for parent in reversed(ancestors(agent_id, agents)):
            ordered.append(parent)
    ordered.append(agent_id)

    if include_children:
        for desc in walk_descendants(agent_id, agents, include_guides=bool(include_guides)):
            if desc not in ordered:
                ordered.append(desc)
    elif include_guides:
        for guide in agents[agent_id].get("guides") or []:
            if guide not in agents:
                raise LoadError(f"{agent_id} guides unknown id: {guide}")
            if guide not in ordered:
                ordered.append(guide)

    return agent_id, ordered


def paths_for_agents(agent_ids: Sequence[str], agents: dict[str, dict[str, Any]]) -> list[str]:
    return [agents[aid]["path"] for aid in agent_ids]


def discover_example_paths(group: str, limit: int) -> list[str]:
    dirname = EXAMPLE_DIR_ALIASES.get(group, group)
    base = ROOT / "examples" / dirname
    if not base.is_dir():
        return []
    files = sorted(p for p in base.glob("*.json") if p.is_file())
    rel = [str(p.relative_to(ROOT)).replace("\\", "/") for p in files[:limit]]
    return rel


def resolve_examples(
    manifest: dict[str, Any],
    groups: Sequence[str],
    *,
    limit: int,
) -> list[str]:
    out: list[str] = []
    manifest_ex = manifest.get("examples") or {}
    for group in groups:
        if group == "all":
            continue
        listed = manifest_ex.get(group) if isinstance(manifest_ex, dict) else None
        if listed:
            paths = [p for p in listed if isinstance(p, str)][:limit]
        else:
            paths = discover_example_paths(group, limit)
        unique_extend(out, paths)
    return out


def parse_example_groups(raw: str | None) -> list[str]:
    if not raw:
        return []
    parts = [p.strip() for p in raw.split(",") if p.strip()]
    if "all" in parts:
        return list(EXAMPLE_GROUPS)
    unknown = [p for p in parts if p not in EXAMPLE_GROUPS]
    if unknown:
        raise LoadError(f"unknown --with-examples group(s): {', '.join(unknown)}")
    return parts


def verify_files(paths: Sequence[str]) -> list[str]:
    missing = []
    for rel in paths:
        if not (ROOT / rel).is_file():
            missing.append(rel)
    return missing


def format_paths(files: Sequence[str], examples: Sequence[str]) -> str:
    lines = list(files) + list(examples)
    return "\n".join(lines) + ("\n" if lines else "")


def format_json(
    *,
    agent: str | None,
    domain: str | None,
    files: Sequence[str],
    examples: Sequence[str],
) -> str:
    payload = {
        "agent": agent,
        "domain": domain,
        "files": list(files),
        "examples": list(examples),
    }
    return json.dumps(payload, ensure_ascii=False, indent=2) + "\n"


def format_bundle(paths: Sequence[str]) -> str:
    chunks: list[str] = []
    for rel in paths:
        text = (ROOT / rel).read_text(encoding="utf-8")
        chunks.append(f"\n\n<!-- BEGIN {rel} -->\n{text.rstrip()}\n<!-- END {rel} -->\n")
    return "".join(chunks)


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="Load JU brain knowledge in Control Tower order (shared-first)."
    )
    p.add_argument("--agent", help="Agent id from knowledge/manifest.json (e.g. ju_orchestrator)")
    p.add_argument(
        "--domain",
        choices=("estimate", "field", "marketing", "shorts", "core"),
        help="Load the chief (or domain root) for this domain",
    )
    p.add_argument(
        "--format",
        choices=("paths", "json", "bundle"),
        default="paths",
        help="Output format (default: paths)",
    )
    p.add_argument(
        "--with-examples",
        metavar="GROUPS",
        help="Comma list: handoff|synthesis|daily_report|shorts_plan|brand_guard|all",
    )
    p.add_argument(
        "--examples-limit",
        type=int,
        default=2,
        help="Max examples per group (default: 2)",
    )
    p.add_argument(
        "--no-ancestors",
        action="store_true",
        help="Do not include reports_to parents",
    )
    p.add_argument(
        "--with-children",
        action="store_true",
        help="Force include children (default on for hq/chief)",
    )
    p.add_argument(
        "--no-children",
        action="store_true",
        help="Do not expand children/guides",
    )
    p.add_argument(
        "--with-guides",
        action="store_true",
        help="Include guides (default on for hq/chief; off for specialist unless set)",
    )
    p.add_argument(
        "--no-guides",
        action="store_true",
        help="Exclude guides even for hq/chief",
    )
    p.add_argument(
        "--self-check",
        action="store_true",
        help="Run built-in load-order assertions then exit",
    )
    p.add_argument(
        "--manifest",
        type=Path,
        default=MANIFEST_PATH,
        help="Path to manifest.json",
    )
    return p


def self_check(manifest: dict[str, Any], agents: dict[str, dict[str, Any]]) -> None:
    shared = shared_paths(manifest)
    if len(shared) < 1:
        raise LoadError("self-check: shared list empty")

    primary, agent_ids = resolve_agent_set(
        agent_id="ju_orchestrator",
        domain=None,
        agents=agents,
        include_ancestors=True,
        include_children=True,
        include_guides=True,
    )
    files = shared + paths_for_agents(agent_ids, agents)
    missing = verify_files(files)
    if missing:
        raise LoadError("self-check missing files: " + ", ".join(missing))

    if files[: len(shared)] != shared:
        raise LoadError("self-check: shared must be first")

    for chief in ("ju_chief_estimate", "ju_chief_field", "ju_chief_marketing"):
        if chief not in agent_ids:
            raise LoadError(f"self-check: orchestrator load missing {chief}")

    # specialist default: no guides unless asked
    _, est_ids = resolve_agent_set(
        agent_id="ju_consultation",
        domain=None,
        agents=agents,
        include_ancestors=True,
        include_children=False,
        include_guides=False,
    )
    if "ju_estimate_guide" in est_ids:
        raise LoadError("self-check: specialist should not load guides by default")

    print("self-check: OK", file=sys.stderr)
    print(format_paths(files, []), end="")


def main(argv: Sequence[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    try:
        manifest = load_manifest(args.manifest)
        agents = agent_index(manifest)

        if args.self_check:
            self_check(manifest, agents)
            return 0

        if not args.agent and not args.domain:
            raise LoadError("--agent is required (or provide --domain)")

        include_children: bool | None
        if args.no_children:
            include_children = False
        elif args.with_children:
            include_children = True
        else:
            include_children = None

        include_guides: bool | None
        if args.no_guides:
            include_guides = False
        elif args.with_guides:
            include_guides = True
        else:
            include_guides = None

        primary, agent_ids = resolve_agent_set(
            agent_id=args.agent,
            domain=args.domain,
            agents=agents,
            include_ancestors=not args.no_ancestors,
            include_children=include_children,
            include_guides=include_guides,
        )

        files: list[str] = []
        unique_extend(files, shared_paths(manifest))
        unique_extend(files, paths_for_agents(agent_ids, agents))

        example_groups = parse_example_groups(args.with_examples)
        examples = resolve_examples(manifest, example_groups, limit=max(0, args.examples_limit))

        all_paths = list(files) + list(examples)
        missing = verify_files(all_paths)
        if missing:
            for m in missing:
                print(f"error: missing file: {m}", file=sys.stderr)
            return 1

        domain = args.domain or (agents[primary].get("domain") if primary else None)

        if args.format == "paths":
            sys.stdout.write(format_paths(files, examples))
        elif args.format == "json":
            sys.stdout.write(
                format_json(agent=primary, domain=domain, files=files, examples=examples)
            )
        else:
            sys.stdout.write(format_bundle(all_paths))
        return 0

    except LoadError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2
    except json.JSONDecodeError as exc:
        print(f"error: invalid manifest JSON: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
