#!/usr/bin/env python3
"""Query OSV for resolved public Maven coordinates; never uploads source or secrets.

Usage: python3 scripts/audit/dependency_scan.py TREE.txt OUTPUT.json
Generate TREE.txt using Maven dependency:tree -DoutputFile=... first.
This is version matching, not proof of application exploitability.
"""
import datetime
import json
import re
import sys
import urllib.request
from pathlib import Path


def main():
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    packages = {}
    for line in Path(sys.argv[1]).read_text().splitlines():
        match = re.search(r"([\w.-]+):([\w.-]+):(jar|pom):(?:(\w[\w.-]*):)?([^: ]+):(compile|runtime|test|provided|system)\b", line)
        if not match:
            continue
        group, artifact, kind, classifier, version, scope = match.groups()
        packages[(group + ":" + artifact, version)] = {"name": group + ":" + artifact, "version": version, "scope": scope}
    if not packages:
        raise SystemExit("No dependencies parsed; refusing to report an empty successful scan.")
    entries = list(packages.values())
    queried_at = datetime.datetime.now(datetime.timezone.utc).isoformat()
    for start in range(0, len(entries), 100):
        batch = entries[start:start + 100]
        body = {"queries": [{"package": {"ecosystem": "Maven", "name": p["name"]}, "version": p["version"]} for p in batch]}
        request = urllib.request.Request("https://api.osv.dev/v1/querybatch", data=json.dumps(body).encode(), headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(request, timeout=45) as response:
            result = json.load(response)
        if len(result.get("results", [])) != len(batch):
            raise SystemExit("Incomplete OSV response; no successful report written.")
        for package, matches in zip(batch, result["results"]):
            if matches.get("next_page_token"):
                raise SystemExit("OSV result requires pagination; refusing an incomplete report.")
            package["advisories"] = matches.get("vulns", [])
    report = {"queried_at": queried_at, "source": "https://api.osv.dev/v1/querybatch", "kind": "version matches; reachability NOT established", "packages": entries}
    Path(sys.argv[2]).parent.mkdir(parents=True, exist_ok=True)
    Path(sys.argv[2]).write_text(json.dumps(report, indent=2) + "\n")
    ids = {v["id"] for p in entries for v in p["advisories"]}
    print(json.dumps({"packages_queried": len(entries), "packages_with_matches": sum(bool(p["advisories"]) for p in entries), "distinct_advisory_ids": len(ids)}))


if __name__ == "__main__":
    main()
