"""Copy resources from a local legal installation; never import game classes."""
import argparse
import os
from pathlib import Path, PurePosixPath
import zipfile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--jar", type=Path, default=Path(os.environ.get("APPDATA", "~")) / ".minecraft/versions/1.8.9/1.8.9.jar")
parser.add_argument("--output", type=Path, default=root / "assets")
args = parser.parse_args()
destination = args.output.resolve()
copied = unchanged = preserved = 0
with zipfile.ZipFile(args.jar) as archive:
    for member in archive.infolist():
        if member.is_dir() or not member.filename.startswith("assets/"):
            continue
        relative = PurePosixPath(member.filename.removeprefix("assets/"))
        if relative.is_absolute() or any(p in (".", "..") for p in relative.parts) or "\\" in str(relative):
            raise ValueError("Unsafe archive resource path")
        target = destination.joinpath(*relative.parts)
        if not target.resolve().is_relative_to(destination):
            raise ValueError("Resource link leaves destination")
        if member.file_size > 32 * 1024 * 1024:
            raise ValueError("Resource exceeds size limit")
        data = archive.read(member)
        if target.exists():
            if target.read_bytes() == data:
                unchanged += 1
            else:
                preserved += 1
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("xb") as output:
            output.write(data)
        copied += 1
print(f"Resources copied={copied}, identical={unchanged}, existing custom files preserved={preserved}")
