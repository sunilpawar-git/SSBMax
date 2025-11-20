#!/usr/bin/env python3
"""
Add missing closing brace to ViewModels that are missing '}'
"""

import sys
from pathlib import Path

def fix_missing_brace(file_path):
    """Add closing brace if missing at end of file"""
    path = Path(file_path)
    if not path.exists():
        print(f"Error: File not found: {file_path}")
        return False
    
    content = path.read_text()
    lines = content.split('\n')
    
    # Check if last non-empty line is missing closing brace
    # Count opening and closing braces
    open_count = content.count('{')
    close_count = content.count('}')
    
    if open_count > close_count:
        missing = open_count - close_count
        print(f"  Missing {missing} closing brace(s)")
        
        # Add closing braces at the end before any trailing newlines
        while lines and not lines[-1].strip():
            lines.pop()
        
        for _ in range(missing):
            lines.append('}')
        
        lines.append('')  # Add trailing newline
        
        new_content = '\n'.join(lines)
        path.write_text(new_content)
        print(f"✅ Fixed: {file_path}")
        return True
    else:
        print(f"  No missing braces: {file_path}")
        return False

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 add_missing_closing_brace.py <file1.kt> [file2.kt ...]")
        sys.exit(1)
    
    files = sys.argv[1:]
    for file_path in files:
        print(f"\nProcessing: {file_path}")
        fix_missing_brace(file_path)


