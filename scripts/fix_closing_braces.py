#!/usr/bin/env python3
"""
Fix closing braces for .update { it.copy(...) } patterns
Converts:
  _uiState.update { it.copy(
      field = value
  )
To:
  _uiState.update { it.copy(
      field = value
  ) }
"""

import sys
import re
from pathlib import Path

def fix_closing_braces(file_path):
    """Fix closing braces in a Kotlin file"""
    path = Path(file_path)
    if not path.exists():
        print(f"Error: File not found: {file_path}")
        return False
    
    content = path.read_text()
    original_content = content
    lines = content.split('\n')
    fixed_lines = []
    
    i = 0
    while i < len(lines):
        line = lines[i]
        
        # Check if this line starts an .update { it.copy( pattern
        if re.search(r'_(ui)?[Ss]tate\.update \{ it\.copy\(', line):
            fixed_lines.append(line)
            i += 1
            
            # Find the matching closing parenthesis
            paren_count = line.count('(') - line.count(')')
            
            while i < len(lines) and paren_count > 0:
                current_line = lines[i]
                paren_count += current_line.count('(') - current_line.count(')')
                
                # If this is the line with the closing paren and it's just ")"
                if paren_count == 0 and current_line.strip() == ')':
                    # Change it to " ) }"
                    indent = len(current_line) - len(current_line.lstrip())
                    fixed_lines.append(' ' * indent + ') }')
                else:
                    fixed_lines.append(current_line)
                
                i += 1
        else:
            fixed_lines.append(line)
            i += 1
    
    new_content = '\n'.join(fixed_lines)
    
    # Write back if changed
    if new_content != original_content:
        path.write_text(new_content)
        print(f"✅ Fixed closing braces in: {file_path}")
        return True
    else:
        print(f"  No changes needed: {file_path}")
        return False

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 fix_closing_braces.py <file1.kt> [file2.kt ...]")
        sys.exit(1)
    
    files = sys.argv[1:]
    for file_path in files:
        print(f"\nProcessing: {file_path}")
        fix_closing_braces(file_path)




