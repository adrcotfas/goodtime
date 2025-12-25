#!/usr/bin/env python3
"""
Script to merge regional variant values directories into their base language directories.

For example:
- values-ar-rSA → values-ar (if values-ar exists)
- values-es-rES → values-es (if values-es exists)
- values-es-rAR → no merge (keep as is for Argentinian Spanish)
- values-zh-rCN → no merge (no values-zh base exists)
"""

import os
import shutil
import re
from pathlib import Path


def merge_values_directories(base_path):
    """
    Merge regional variant directories into their base language directories.

    Args:
        base_path: Path to the composeResources directory
    """
    base_path = Path(base_path)

    if not base_path.exists():
        print(f"Error: Path {base_path} does not exist")
        return

    # Exceptions: directories that should NOT be merged even if base exists
    # These are distinct regional variants that should remain separate
    MERGE_EXCEPTIONS = {
        'values-es-rAR',  # Argentinian Spanish (distinct from European Spanish)
        'values-pt-rBR',  # Brazilian Portuguese (distinct from European Portuguese)
    }

    # Find all directories matching the pattern "values-XX-rYY"
    regional_dirs = []
    for item in base_path.iterdir():
        if item.is_dir() and re.match(r'values-.+-r[A-Z]{2}$', item.name):
            regional_dirs.append(item)

    # Sort for consistent processing
    regional_dirs.sort(key=lambda x: x.name)

    print(f"Found {len(regional_dirs)} regional variant directories\n")

    merged_count = 0
    skipped_count = 0
    exception_count = 0

    for regional_dir in regional_dirs:
        # Check if this directory is in the exceptions list
        if regional_dir.name in MERGE_EXCEPTIONS:
            print(f"⊘ Skipping: {regional_dir.name} (exception: distinct regional variant)")
            exception_count += 1
            continue
        # Extract base language code (everything before "-r")
        match = re.match(r'(values-.+)-r[A-Z]{2}$', regional_dir.name)
        if not match:
            continue

        base_name = match.group(1)
        base_dir = base_path / base_name

        # Check if base directory exists
        if base_dir.exists() and base_dir.is_dir():
            print(f"✓ Merging: {regional_dir.name} → {base_name}")

            # Move all files from regional dir to base dir
            files_moved = 0
            for file_path in regional_dir.iterdir():
                if file_path.is_file():
                    dest_path = base_dir / file_path.name

                    # If file exists in destination, warn user
                    if dest_path.exists():
                        print(f"  ⚠ Warning: {file_path.name} already exists in {base_name}, overwriting...")

                    shutil.move(str(file_path), str(dest_path))
                    files_moved += 1

            print(f"  Moved {files_moved} file(s)")

            # Remove the now-empty regional directory
            regional_dir.rmdir()
            print(f"  Removed empty directory: {regional_dir.name}\n")

            merged_count += 1
        else:
            print(f"⊘ Skipping: {regional_dir.name} (no base directory '{base_name}' found)")
            skipped_count += 1

    print(f"\n{'='*60}")
    print(f"Summary:")
    print(f"  Merged: {merged_count} directories")
    print(f"  Skipped: {skipped_count} directories (no base directory)")
    print(f"  Exceptions: {exception_count} directories (distinct regional variants)")
    print(f"{'='*60}")


if __name__ == "__main__":
    # Path to composeResources (relative to scripts directory)
    compose_resources_path = "../composeApp/src/commonMain/composeResources"

    print("Regional Values Directory Merger")
    print("="*60)
    print(f"Target path: {compose_resources_path}\n")

    # Confirm before proceeding
    response = input("This will move files and delete directories. Continue? (yes/no): ")

    if response.lower() in ['yes', 'y']:
        merge_values_directories(compose_resources_path)
        print("\nDone!")
    else:
        print("Operation cancelled.")
