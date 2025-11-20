#!/usr/bin/env python3
"""
Bulk refactor StateFlow patterns in Kotlin files
Converts: _uiState.value = _uiState.value.copy(...) 
To: _uiState.update { it.copy(...) }
"""

import sys
import re
from pathlib import Path

def refactor_file(file_path):
    """Refactor a single Kotlin file"""
    path = Path(file_path)
    if not path.exists():
        print(f"Error: File not found: {file_path}")
        return False
    
    # Read content
    content = path.read_text()
    original_content = content
    
    # Add import if needed
    if 'import kotlinx.coroutines.flow.update' not in content:
        # Find last kotlinx.coroutines.flow import
        import_pattern = r'(import kotlinx\.coroutines\.flow\.[^\n]+\n)'
        matches = list(re.finditer(import_pattern, content))
        if matches:
            last_match = matches[-1]
            insert_pos = last_match.end()
            content = (content[:insert_pos] + 
                      'import kotlinx.coroutines.flow.update\n' + 
                      content[insert_pos:])
            print(f"  ✓ Added update import")
    
    # Pattern 1: Simple single-line replacements
    # _uiState.value = _uiState.value.copy(...)
    pattern1 = r'_uiState\.value = _uiState\.value\.copy\('
    replacement1 = r'_uiState.update { it.copy('
    content, count1 = re.subn(pattern1, replacement1, content)
    
    # Pattern 2: _state.value = _state.value.copy(...)
    pattern2 = r'_state\.value = _state\.value\.copy\('
    replacement2 = r'_state.update { it.copy('
    content, count2 = re.subn(pattern2, replacement2, content)
    
    total_replacements = count1 + count2
    
    if total_replacements > 0:
        print(f"  ✓ Replaced {total_replacements} patterns")
        print("  ⚠️  Review and manually fix closing braces: ) should become ) }")
    
    # Write back if changed
    if content != original_content:
        path.write_text(content)
        print(f"✅ Refactored: {file_path}")
        return True
    else:
        print(f"  No changes needed: {file_path}")
        return False

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 bulk_refactor.py <file1.kt> [file2.kt ...]")
        sys.exit(1)
    
    files = sys.argv[1:]
    for file_path in files:
        print(f"\nProcessing: {file_path}")
        refactor_file(file_path)

