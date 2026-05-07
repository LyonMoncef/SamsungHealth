---
type: code-source
language: python
file_path: agents/mcp/android_test_runner/server.py
git_blob: 894d36250ab6d2933f0e44eb9219e1386df3f74f
last_synced: '2026-05-06T23:16:50Z'
loc: 327
annotations: []
imports:
- os
- re
- subprocess
- pathlib
- mcp.server.fastmcp
exports: []
tags:
- code
- python
---

# agents/mcp/android_test_runner/server.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/mcp/android_test_runner/server.py`](../../../agents/mcp/android_test_runner/server.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""
MCP server — Android Test Runner
Deterministic tools for Gradle env setup, dep management, test execution, result parsing.
All reasoning stays in the agent; this server handles only mechanical, repeatable operations.
"""

import os
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

from mcp.server.fastmcp import FastMCP

PROJECT_ROOT = Path(__file__).parent.parent.parent.parent  # SamsungHealth/
ANDROID_DIR = PROJECT_ROOT / "android-app"
APP_BUILD_GRADLE = ANDROID_DIR / "app" / "build.gradle.kts"

mcp = FastMCP("android-test-runner")

# ──────────────────────────────────────────────
# 1. Environment check
# ──────────────────────────────────────────────

@mcp.tool()
def android_check_env() -> dict:
    """
    Check JDK version, Android SDK path, and Gradle wrapper availability.
    Returns a structured dict — no side effects.
    """
    result = {"ok": True, "issues": []}

    # JDK
    try:
        out = subprocess.check_output(["java", "-version"], stderr=subprocess.STDOUT, text=True)
        version_line = out.strip().splitlines()[0]
        result["java_version"] = version_line
        if "17" not in version_line and "21" not in version_line and "11" not in version_line:
            result["issues"].append(f"JDK version may be unsupported: {version_line}")
    except FileNotFoundError:
        result["ok"] = False
        result["issues"].append("java not found — install JDK 17+")
        result["java_version"] = None

    # Gradle wrapper
    gradlew = ANDROID_DIR / "gradlew"
    result["gradlew_present"] = gradlew.exists()
    if not gradlew.exists():
        result["ok"] = False
        result["issues"].append("gradlew not found in android-app/")

    # local.properties / SDK
    local_props = ANDROID_DIR / "local.properties"
    result["local_properties_present"] = local_props.exists()
    sdk_dir = None
    if local_props.exists():
        for line in local_props.read_text().splitlines():
            if line.startswith("sdk.dir="):
                sdk_dir = line.split("=", 1)[1].strip()
                break
    if sdk_dir is None:
        # try env
        sdk_dir = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    result["sdk_dir"] = sdk_dir
    if sdk_dir and not Path(sdk_dir).exists():
        result["issues"].append(f"sdk.dir points to non-existent path: {sdk_dir}")

    return result


# ──────────────────────────────────────────────
# 2. Dependency audit
# ──────────────────────────────────────────────

REQUIRED_TEST_DEPS = {
    "junit:junit:4.13.2":                                          "testImplementation",
    "app.cash.paparazzi:paparazzi:1.3.4":                         "testImplementation",
    "org.mockito.kotlin:mockito-kotlin:5.3.1":                     "testImplementation",
    "com.squareup.okhttp3:mockwebserver:4.12.0":                   "testImplementation",
    "org.robolectric:robolectric:4.12.2":                          "testImplementation",
    "androidx.navigation:navigation-testing:2.8.5":                "testImplementation",
    "androidx.compose.ui:ui-test-junit4":                          "testImplementation",
    "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0":         "testImplementation",
    "androidx.arch.core:core-testing:2.2.0":                       "testImplementation",
}

REQUIRED_TEST_PLUGIN = "app.cash.paparazzi"


@mcp.tool()
def android_audit_test_deps() -> dict:
    """
    Read build.gradle.kts and return which required test dependencies and plugins are missing.
    No side effects.
    """
    if not APP_BUILD_GRADLE.exists():
        return {"error": f"build.gradle.kts not found at {APP_BUILD_GRADLE}"}

    content = APP_BUILD_GRADLE.read_text()
    missing_deps = []
    present_deps = []

    for dep, scope in REQUIRED_TEST_DEPS.items():
        # match both quoted forms: "group:artifact:version" and (group:artifact) without version
        artifact = dep.rsplit(":", 1)[0]  # strip version for flexible match
        if artifact in content or dep in content:
            present_deps.append(dep)
        else:
            missing_deps.append({"dep": dep, "scope": scope})

    plugin_present = REQUIRED_TEST_PLUGIN in content

    return {
        "missing_deps": missing_deps,
        "present_deps": present_deps,
        "paparazzi_plugin_present": plugin_present,
        "build_gradle_path": str(APP_BUILD_GRADLE),
    }


# ──────────────────────────────────────────────
# 3. Add missing deps
# ──────────────────────────────────────────────

@mcp.tool()
def android_add_test_deps(deps: list[dict], add_paparazzi_plugin: bool = False) -> dict:
    """
    Add missing testImplementation lines to build.gradle.kts.
    deps: list of {"dep": "group:artifact:version", "scope": "testImplementation"}
    Returns lines added. Idempotent — skips already-present deps.
    """
    if not APP_BUILD_GRADLE.exists():
        return {"error": f"build.gradle.kts not found at {APP_BUILD_GRADLE}"}

    content = APP_BUILD_GRADLE.read_text()
    added = []

    # Add plugin if needed
    if add_paparazzi_plugin and REQUIRED_TEST_PLUGIN not in content:
        # Insert after last plugin declaration
        content = re.sub(
            r'(plugins\s*\{[^}]*)(})',
            lambda m: m.group(1) + f'    id("app.cash.paparazzi") version "1.3.4"\n' + m.group(2),
            content,
            count=1,
            flags=re.DOTALL,
        )
        # Also add to root build.gradle.kts classpath — note only in app build
        added.append(f'plugin: id("app.cash.paparazzi") version "1.3.4"')

    # Add missing testImplementation lines
    for entry in deps:
        dep = entry["dep"]
        scope = entry.get("scope", "testImplementation")
        artifact = dep.rsplit(":", 1)[0]
        if artifact in content or dep in content:
            continue  # already present
        dep_line = f'    {scope}("{dep}")'
        # Insert before closing brace of dependencies block
        content = re.sub(
            r'(dependencies\s*\{)(.*?)(^\})',
            lambda m: m.group(1) + m.group(2) + dep_line + "\n" + m.group(3),
            content,
            count=1,
            flags=re.DOTALL | re.MULTILINE,
        )
        added.append(dep_line.strip())

    APP_BUILD_GRADLE.write_text(content)

    return {
        "added": added,
        "build_gradle_path": str(APP_BUILD_GRADLE),
        "total_added": len(added),
    }


# ──────────────────────────────────────────────
# 4. Run tests
# ──────────────────────────────────────────────

@mcp.tool()
def android_run_tests(
    flavor: str = "webview",
    build_type: str = "Debug",
    timeout_seconds: int = 300,
) -> dict:
    """
    Run ./gradlew :app:test<Flavor><BuildType>UnitTest --continue in android-app/.
    Returns stdout tail, return code, and whether XML results were produced.
    """
    task = f":app:test{flavor.capitalize()}{build_type.capitalize()}UnitTest"
    gradlew = ANDROID_DIR / "gradlew"

    if not gradlew.exists():
        return {"error": "gradlew not found", "task": task}

    try:
        proc = subprocess.run(
            [str(gradlew), task, "--continue", "--no-daemon"],
            cwd=str(ANDROID_DIR),
            capture_output=True,
            text=True,
            timeout=timeout_seconds,
        )
        stdout_tail = proc.stdout[-4000:] if len(proc.stdout) > 4000 else proc.stdout
        stderr_tail = proc.stderr[-2000:] if len(proc.stderr) > 2000 else proc.stderr

        results_dir = ANDROID_DIR / "app" / "build" / "test-results" / f"test{flavor.capitalize()}{build_type.capitalize()}UnitTest"
        xml_files = list(results_dir.glob("*.xml")) if results_dir.exists() else []

        return {
            "task": task,
            "return_code": proc.returncode,
            "build_failed": proc.returncode != 0,
            "stdout_tail": stdout_tail,
            "stderr_tail": stderr_tail,
            "xml_results_dir": str(results_dir),
            "xml_files_count": len(xml_files),
        }
    except subprocess.TimeoutExpired:
        return {"error": f"Gradle timed out after {timeout_seconds}s", "task": task}


# ──────────────────────────────────────────────
# 5. Parse XML test results
# ──────────────────────────────────────────────

@mcp.tool()
def android_parse_results(
    flavor: str = "webview",
    build_type: str = "Debug",
) -> dict:
    """
    Parse JUnit XML results from build/test-results/.
    Returns counts: red (failures+errors), green (passed), skipped, total.
    """
    results_dir = (
        ANDROID_DIR / "app" / "build" / "test-results"
        / f"test{flavor.capitalize()}{build_type.capitalize()}UnitTest"
    )

    if not results_dir.exists():
        return {
            "error": "No XML results found — build likely failed before test execution",
            "compile_error": True,
            "red": 0,
            "green": 0,
            "skipped": 0,
            "total": 0,
            "results_dir": str(results_dir),
        }

    red = green = skipped = total = 0
    failures = []

    for xml_file in sorted(results_dir.glob("*.xml")):
        try:
            tree = ET.parse(xml_file)
            root = tree.getroot()
            for tc in root.iter("testcase"):
                total += 1
                if tc.find("failure") is not None or tc.find("error") is not None:
                    red += 1
                    name = tc.get("classname", "") + "." + tc.get("name", "")
                    failures.append(name)
                elif tc.find("skipped") is not None:
                    skipped += 1
                else:
                    green += 1
        except ET.ParseError:
            pass

    return {
        "red": red,
        "green": green,
        "skipped": skipped,
        "total": total,
        "compile_error": False,
        "failing_tests": failures[:20],  # cap to avoid huge output
        "results_dir": str(results_dir),
    }


# ──────────────────────────────────────────────
# 6. Update spec tested_by frontmatter
# ──────────────────────────────────────────────

@mcp.tool()
def android_update_spec_tested_by(spec_path: str, test_files: list[str]) -> dict:
    """
    Update tested_by: list in a spec's YAML frontmatter.
    Merges with existing entries — idempotent.
    """
    path = PROJECT_ROOT / spec_path if not Path(spec_path).is_absolute() else Path(spec_path)

    if not path.exists():
        return {"error": f"Spec not found: {spec_path}"}

    content = path.read_text()

    # Extract existing tested_by
    existing: list[str] = []
    match = re.search(r'^tested_by:\s*\[(.*?)\]', content, re.MULTILINE)
    if match:
        existing = [x.strip().strip('"\'') for x in match.group(1).split(',') if x.strip()]

    merged = sorted(set(existing + test_files))
    new_value = "[" + ", ".join(f'"{f}"' for f in merged) + "]"

    # Replace inline list form
    content = re.sub(r'^tested_by:.*$', f'tested_by: {new_value}', content, flags=re.MULTILINE)
    path.write_text(content)

    return {
        "spec_path": str(path),
        "tested_by": merged,
        "added": [f for f in test_files if f not in existing],
    }


# ──────────────────────────────────────────────
# Entrypoint
# ──────────────────────────────────────────────

if __name__ == "__main__":
    mcp.run()
```

---

## Appendix — symbols & navigation *(auto)*

### Imports
- `os`
- `re`
- `subprocess`
- `pathlib`
- `mcp.server.fastmcp`
