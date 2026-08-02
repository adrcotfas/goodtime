#!/usr/bin/env python3
"""
Script to replace escaped apostrophes (\') with regular apostrophes (') in string resource files.
"""

import os
import re
from pathlib import Path

STRING_BODY_RE = re.compile(r'(?<=>)([^<]+)(?=</string>)')


def fix_string_body(body):
    body = body.replace(r"\'", "'")
    if body.count(r'\"') == 1 and body.endswith('\\'):
        # truncated pair: \"text\ -> "text"
        body = body.replace(r'\"', '"')[:-1] + '"'
    elif body.count(r'\"') % 2 == 0:
        body = body.replace(r'\"', '"')
    else:
        # stray unpaired \" -> drop it
        body = body.replace(r'\"', '')
    return body


def replace_escaped_apostrophes(base_path):
    """
    Replace \' with ' in all string XML files.

    Args:
        base_path: Path to the composeResources directory
    """
    base_path = Path(base_path)

    if not base_path.exists():
        print(f"Error: Path {base_path} does not exist")
        return

    # Find all values directories
    values_dirs = []
    for item in base_path.iterdir():
        if item.is_dir() and item.name.startswith('values'):
            values_dirs.append(item)

    # Sort for consistent processing
    values_dirs.sort(key=lambda x: x.name)

    print(f"Found {len(values_dirs)} values directories\n")

    total_files_processed = 0
    total_files_modified = 0
    total_replacements = 0

    for values_dir in values_dirs:
        # Find all XML files in this directory
        xml_files = list(values_dir.glob('*.xml'))

        if not xml_files:
            continue

        print(f"Processing: {values_dir.name}")
        dir_replacements = 0

        for xml_file in xml_files:
            total_files_processed += 1

            # Read file content
            with open(xml_file, 'r', encoding='utf-8') as f:
                content = f.read()

            file_replacements = 0

            def repl(match):
                nonlocal file_replacements
                fixed = fix_string_body(match.group(1))
                if fixed != match.group(1):
                    file_replacements += 1
                return fixed

            new_content = STRING_BODY_RE.sub(repl, content)

            if file_replacements > 0:

                # Write back
                with open(xml_file, 'w', encoding='utf-8') as f:
                    f.write(new_content)

                print(f"  ✓ {xml_file.name}: {file_replacements} replacement(s)")
                total_files_modified += 1
                total_replacements += file_replacements
                dir_replacements += file_replacements

        if dir_replacements > 0:
            print(f"  Total: {dir_replacements} replacement(s) in {values_dir.name}\n")

    print(f"{'='*60}")
    print(f"Summary:")
    print(f"  Files processed: {total_files_processed}")
    print(f"  Files modified: {total_files_modified}")
    print(f"  Total replacements: {total_replacements}")
    print(f"{'='*60}")


if __name__ == "__main__":
    # Path to composeResources (relative to scripts directory)
    compose_resources_path = "../shared/src/commonMain/composeResources"

    print("Escaped Apostrophe Replacer")
    print("="*60)
    print(f"Target path: {compose_resources_path}")
    print(f"Will replace: \\' → '\n")

    # Confirm before proceeding
    response = input("This will modify XML files. Continue? (yes/no): ")

    if response.lower() in ['yes', 'y']:
        replace_escaped_apostrophes(compose_resources_path)
        print("\nDone!")
    else:
        print("Operation cancelled.")
