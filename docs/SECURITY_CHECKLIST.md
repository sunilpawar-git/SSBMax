# SSBMax Security Implementation Checklist

## 🔒 Test ViewModel Security Requirements

This checklist MUST be completed for **EVERY** test ViewModel to prevent subscription bypass vulnerabilities.

---

## Current Security Status

| Test Type | ViewModel Exists | Authentication Guard | Subscription Check | Usage Recording | Security Logging | Status |
|-----------|------------------|---------------------|-------------------|-----------------|------------------|--------|
| **OIR** | ✅ | ✅ | ✅ | ✅ | ✅ | **SECURE** |
| **WAT** | ✅ | ✅ | ✅ | ✅ | ✅ | **SECURE** |
| **SRT** | ✅ | ✅ | ✅ | ✅ | ✅ | **SECURE** |
| **TAT** | ✅ | ✅ | ✅ | ✅ | ✅ | **SECURE** |
| **PPDT** | ✅ | ✅ | ✅ | ✅ | ✅ | **SECURE** |
| **GTO** | ❌ | ❌ | ❌ | ❌ | ❌ | **NOT IMPLEMENTED** |
| **IO (Interview)** | ❌ | ❌ | ❌ | ❌ | ❌ | **NOT IMPLEMENTED** |
| **SD (Self Desc)** | ❌ | ❌ | ❌ | ❌ | ❌ | **NOT IMPLEMENTED** |

---

## 📋 Implementation Steps (Copy-Paste Ready)

When creating GTO/IO/SD ViewModels, follow these steps:

### Step 1: Authentication Guard
See: core/domain/src/main/kotlin/com/ssbmax/core/domain/model/SSBPhase.kt (lines 24-32)

### Step 2: Subscription Check  
Reference: app/src/main/kotlin/com/ssbmax/ui/tests/tat/TATTestViewModel.kt

### Step 3: Usage Recording
Reference: app/src/main/kotlin/com/ssbmax/ui/tests/ppdt/PPDTTestViewModel.kt

### Step 4: Unit Tests
Reference: app/src/test/kotlin/com/ssbmax/ui/tests/wat/WATTestViewModelTest.kt

---

## ⚠️ CRITICAL WARNING

Implementing a new test ViewModel without these security measures will:
- ❌ Allow unlimited test attempts (bypass subscription)
- ❌ Allow unauthenticated access
- ❌ Break analytics tracking
- ❌ Create revenue loss

---

**Last Updated**: November 6, 2025
**Secure**: OIR, WAT, SRT, TAT, PPDT
**Pending**: GTO, IO, SD
