"""Publish a plugin APK to an ATAK update repository (a folder TAK serves).

ATAK's Package Management reads <repo>/product.infz (a zip holding
product.inf and icons) or <repo>/product.inf. The line format below was read
from ATAK-CIV 5.8.0.10's com.atakmap.android.update.ProductInformation:
comma-separated, 12 or 13 columns, '#' lines ignored, SHA-256 hash:

    platform,type,package,name,version,revision,apk,icon,description,
    sha256,min_android_sdk,tak_requirement[,size_bytes]

This script adds or replaces one package's line and keeps every other line,
so other plugins in the same repository are left alone.

Usage (on the server, after copying the APK and icon into the repo):
    python3 publish_update.py REPO_DIR APK_NAME --package P --name N
        --version V --revision R --icon ICON_NAME --description D
        --min-sdk 21 --tak-req com.atakmap.app@5.8.0.CIV
"""

from __future__ import annotations

import argparse
import hashlib
import os
import sys
import tempfile
import zipfile
from pathlib import Path

HEADER = ("#platform,type,full_package_name,display_name,version,revision_code,"
          "apk_url,icon_url,description,apk_hash,os_requirement,tak_prereq,apk_size")


def clean(field: str, name: str) -> str:
    if "," in field or "\n" in field or "\r" in field:
        raise SystemExit(f"{name} must not contain commas or line breaks: {field!r}")
    return field.strip()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("repo")
    ap.add_argument("apk")
    ap.add_argument("--package", required=True)
    ap.add_argument("--name", required=True)
    ap.add_argument("--version", required=True)
    ap.add_argument("--revision", type=int, required=True)
    ap.add_argument("--icon", default="")
    ap.add_argument("--description", default="")
    ap.add_argument("--min-sdk", type=int, default=21)
    ap.add_argument("--tak-req", required=True)
    ap.add_argument("--type", default="plugin", choices=["plugin", "app"])
    a = ap.parse_args()

    repo = Path(a.repo)
    apk = repo / a.apk
    if not apk.is_file():
        raise SystemExit(f"{apk} not found; copy the APK into the repository first")
    if a.icon and not (repo / a.icon).is_file():
        raise SystemExit(f"{repo / a.icon} not found")
    data = apk.read_bytes()
    line = ",".join([
        "Android", a.type, clean(a.package, "package"), clean(a.name, "name"),
        clean(a.version, "version"), str(a.revision), clean(a.apk, "apk"),
        clean(a.icon, "icon"), clean(a.description, "description"),
        hashlib.sha256(data).hexdigest(), str(a.min_sdk), clean(a.tak_req, "tak-req"),
        str(len(data)),
    ])

    index = repo / "product.inf"
    lines = []
    if index.is_file():
        for old in index.read_text(encoding="utf-8").splitlines():
            if not old.strip() or old.startswith("#"):
                continue
            cols = old.split(",")
            if len(cols) > 2 and cols[2] == a.package:
                continue  # replaced below
            lines.append(old)
    lines.append(line)
    text = HEADER + "\n" + "\n".join(lines) + "\n"

    # Write atomically so ATAK never reads a half-written index.
    def atomic_write(path: Path, payload: bytes) -> None:
        fd, tmp = tempfile.mkstemp(dir=repo, prefix=".tmp-")
        with os.fdopen(fd, "wb") as f:
            f.write(payload)
        os.chmod(tmp, 0o664)
        os.replace(tmp, path)

    atomic_write(index, text.encode("utf-8"))
    fd, tmpz = tempfile.mkstemp(dir=repo, prefix=".tmp-")
    os.close(fd)
    with zipfile.ZipFile(tmpz, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("product.inf", text)
        for other in lines:
            icon = other.split(",")[7] if len(other.split(",")) > 7 else ""
            if icon and (repo / icon).is_file() and icon not in z.namelist():
                z.write(repo / icon, icon)
    os.chmod(tmpz, 0o664)
    os.replace(tmpz, repo / "product.infz")
    print(f"published {a.package} {a.version} (revision {a.revision}); "
          f"{len(lines)} product(s) in the index")
    return 0


if __name__ == "__main__":
    sys.exit(main())
