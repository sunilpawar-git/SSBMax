#!/usr/bin/env python3
import os
from pathlib import Path

# Find all ViewModels
vm_dir = Path("app/src/main/kotlin")
test_dir = Path("app/src/test/kotlin")

viewmodels = set()
for vm_file in vm_dir.rglob("*ViewModel.kt"):
    vm_name = vm_file.stem  # filename without .kt
    viewmodels.add(vm_name)

# Find all test files
test_files = set()
for test_file in test_dir.rglob("*ViewModelTest.kt"):
    test_name = test_file.stem.replace("Test", "")  # Remove "Test" suffix
    test_files.add(test_name)

# Find missing
missing = sorted(viewmodels - test_files)
covered = sorted(viewmodels & test_files)

print("━" * 70)
print("🔍 VIEWMODEL TEST COVERAGE ANALYSIS")
print("━" * 70)
print(f"\nTotal ViewModels: {len(viewmodels)}")
print(f"✅ ViewModels with tests: {len(covered)}")
print(f"❌ ViewModels missing tests: {len(missing)}")
print(f"📊 Test coverage: {len(covered) * 100 // len(viewmodels)}%")

if missing:
    print("\n" + "━" * 70)
    print("❌ ViewModels MISSING Tests:")
    print("━" * 70)
    for i, vm in enumerate(missing, 1):
        print(f"{i:2}. {vm}")
else:
    print("\n🎉 ALL VIEWMODELS HAVE TESTS!")

if covered:
    print("\n" + "━" * 70)
    print("✅ ViewModels WITH Tests:")
    print("━" * 70)
    for i, vm in enumerate(covered, 1):
        print(f"{i:2}. {vm}")
