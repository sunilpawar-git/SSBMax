# SSBMax Memory Profiling Guidelines

## Overview

Memory leaks in Android applications can cause poor performance, crashes, and bad user experience. SSBMax, being a test-taking app where users spend extended periods (30-60 minutes), must be particularly vigilant about memory management.

This guide explains how to detect, analyze, and fix memory leaks using Android Studio's Memory Profiler and LeakCanary.

## 🛠️ Tools Overview

### 1. LeakCanary (Automatic Detection)
- **Integrated**: Already configured in debug builds (`leakcanary-android:2.14`)
- **When it runs**: Automatically on app launch in debug mode
- **What it does**: Monitors object references and detects leaks
- **How to see results**: Check Logcat for "LeakCanary" messages or notification

### 2. Android Studio Memory Profiler (Manual Analysis)
- **Location**: Android Studio → View → Tool Windows → Profiler
- **Purpose**: Detailed memory analysis and heap dumps
- **Best for**: Deep analysis of memory usage patterns

## 🚨 LeakCanary Usage

### Automatic Leak Detection

LeakCanary runs automatically in debug builds. When it detects a leak:

1. **Notification**: You'll see a system notification
2. **Logcat**: Look for messages like:
   ```
   LeakCanary: Leak detected in heap dump
   LeakCanary: 1 APPLICATION LEAKS
   ```

3. **Leak Details**: Tap notification to see:
   - Which object leaked
   - Reference chain showing why it wasn't GC'd
   - Stack trace of where the leak occurred

### Interpreting LeakCanary Results

```
┬───
│ GC Root: Thread object
│
├─ thread MyViewModel$special$$inlined$collect$1
│    Leaking: YES (ObjectWatcher was watching this)
│    Retaining 2.1 kB in 45 objects
│    Anonymous subclass of kotlinx.coroutines.JobSupport
│    ↓ JobSupport.parentHandle
│                ~~~~~~~~~~~~~~
├─ MyViewModel instance
│    Leaking: YES (ViewModel.mBagOfTags below)
│    ↓ ViewModel.mBagOfTags
│                ~~~~~~~~~~~
╰→ MainActivity instance
     Leaking: YES (Activity.mDestroyed)
```

**Analysis:**
- `MyViewModel` holds reference to `MainActivity`
- `MainActivity` is destroyed (`mDestroyed = true`)
- ViewModel prevents Activity from being garbage collected
- **Result**: Memory leak of 2.1 kB

### Common SSBMax Leak Patterns

#### 1. ViewModel Holding Activity Reference
```kotlin
// ❌ BAD - Direct Activity reference
class MyViewModel : ViewModel() {
    private var activity: MainActivity? = null

    fun setActivity(activity: MainActivity) {
        this.activity = activity
    }
}
```

```kotlin
// ✅ GOOD - WeakReference or no reference
class MyViewModel : ViewModel() {
    private var activityRef = WeakReference<MainActivity>(null)

    fun setActivity(activity: MainActivity) {
        this.activityRef = WeakReference(activity)
    }
}
```

#### 2. Coroutine Capturing Outer Class
```kotlin
// ❌ BAD - Captures Activity reference
class MyActivity : AppCompatActivity() {
    fun startBackgroundWork() {
        lifecycleScope.launch {
            // This closure captures 'this' (Activity)
            doWork()
        }
    }
}
```

```kotlin
// ✅ GOOD - Use ViewModel scope
class MyViewModel : ViewModel() {
    fun startBackgroundWork() {
        viewModelScope.launch {
            // No Activity reference captured
            doWork()
        }
    }
}
```

#### 3. Listener Not Removed
```kotlin
// ❌ BAD - Listener not cleaned up
class MyViewModel : ViewModel() {
    private val listener = object : SomeListener {
        override fun onEvent() { /* handle event */ }
    }

    init {
        someService.addListener(listener) // Added but never removed
    }
}
```

```kotlin
// ✅ GOOD - Proper cleanup
class MyViewModel : ViewModel() {
    private val listener = object : SomeListener {
        override fun onEvent() { /* handle event */ }
    }

    init {
        someService.addListener(listener)
    }

    override fun onCleared() {
        super.onCleared()
        someService.removeListener(listener) // Proper cleanup
    }
}
```

## 📊 Android Studio Memory Profiler

### Starting a Memory Profiling Session

1. **Launch app** in debug mode
2. **Open Profiler**: Android Studio → View → Tool Windows → Profiler
3. **Select device**: Choose your test device/emulator
4. **Click Memory**: Start memory profiling session

### Memory Profiler Interface

```
┌─────────────────────────────────────────────────┐
│ [CPU] [MEMORY] [NETWORK] [ENERGY]               │
│                                                 │
│ Memory Usage Graph:                             │
│ ▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔ │
│                                                 │
│ Allocated: 25 MB     Used: 18 MB     Available │
│                                                 │
│ [RECORD] [GC] [HEAP DUMP] [ALLOCATION TRACKER] │
└─────────────────────────────────────────────────┘
```

### Recording Memory Allocations

1. **Click "Record"**: Start recording allocations
2. **Perform actions**: Navigate through the app (e.g., start a test)
3. **Click "Stop"**: End recording
4. **Analyze results**: See what objects were allocated

### Taking Heap Dumps

1. **Click "Heap Dump"**: Capture current heap state
2. **Wait for analysis**: Studio analyzes the heap
3. **Browse objects**: See all objects in memory and their sizes

### Analyzing Heap Dump Results

**Key columns:**
- **Class Name**: Type of object
- **Count**: Number of instances
- **Total Size**: Memory used by all instances
- **Shallow Size**: Memory used by object itself
- **Retained Size**: Memory that would be freed if object was GC'd

**Finding leaks:**
1. Look for unexpected object counts (e.g., 100 Activity instances)
2. Check for large Retained Sizes on destroyed objects
3. Use "References" tab to see what holds references to leaking objects

## 🔍 SSBMax-Specific Memory Issues

### Critical Test Scenarios to Monitor

#### 1. TAT Test Memory Usage
```kotlin
// Monitor during TAT test:
// - Image loading and caching
// - Story input field memory
// - Timer coroutine lifecycle
// - Auto-save functionality
```

**Common leaks:**
- Bitmap caching without proper cleanup
- Timer Job not cancelled on test completion
- Text input buffer accumulation

#### 2. WAT Test Rapid Word Display
```kotlin
// Monitor during WAT test:
// - Word list memory usage
// - Rapid UI updates (15s intervals)
// - Response storage accumulation
```

**Common leaks:**
- Word list not cleared between tests
- Response history growing indefinitely
- Timer coroutine leaks

#### 3. Long-Running Interview Sessions
```kotlin
// Monitor during interviews:
// - Audio recording buffers
- Real-time transcription memory
- WebSocket connection handling
- Question cache management
```

**Common leaks:**
- Audio buffer accumulation
- WebSocket listeners not cleaned up
- Cached questions not evicted

### Firebase Integration Memory Issues

```kotlin
// Monitor Firebase operations:
// - Firestore listeners
// - Storage download/upload streams
// - Authentication state listeners
```

**Common leaks:**
```kotlin
// ❌ BAD - Listener not removed
val listener = object : EventListener<DocumentSnapshot> { ... }
db.collection("users").document(userId)
    .addSnapshotListener(listener) // Never removed

// ✅ GOOD - Proper cleanup
override fun onCleared() {
    super.onCleared()
    db.collection("users").document(userId)
        .removeSnapshotListener(listener)
}
```

## 🐛 Debugging Memory Leaks

### Step-by-Step Leak Investigation

1. **Reproduce the leak**
   - Perform specific user flow (e.g., start TAT test, rotate device, exit)
   - Use LeakCanary to detect automatic leaks

2. **Analyze with Memory Profiler**
   - Take heap dump before and after suspected leak
   - Compare object counts and retained sizes
   - Look for growing object counts

3. **Trace reference chains**
   - Use "References" tab in heap dump
   - Find what holds references to leaking objects
   - Look for unexpected strong references

4. **Fix the leak**
   - Use WeakReference for UI component references
   - Ensure proper cleanup in `onCleared()`
   - Remove listeners in `onDestroy()` or `onCleared()`

### Common Fix Patterns

#### WeakReference Pattern
```kotlin
class MyViewModel : ViewModel() {
    private var contextRef = WeakReference<Context>(null)

    fun setContext(context: Context) {
        contextRef = WeakReference(context)
    }

    fun doSomethingNeedingContext() {
        contextRef.get()?.let { context ->
            // Use context safely - may be null if GC'd
        }
    }
}
```

#### Lifecycle-Aware Cleanup
```kotlin
class MyViewModel : ViewModel() {
    private val compositeDisposable = CompositeDisposable()

    init {
        // Add disposables
        someObservable
            .subscribe()
            .addTo(compositeDisposable)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear() // Clean up all subscriptions
    }
}
```

#### Event Listener Cleanup
```kotlin
class MyViewModel : ViewModel() {
    private val listeners = mutableListOf<Listener>()

    fun addListener(listener: Listener) {
        listeners.add(listener)
        someService.addListener(listener)
    }

    override fun onCleared() {
        super.onCleared()
        listeners.forEach { someService.removeListener(it) }
        listeners.clear()
    }
}
```

## 📈 Performance Monitoring

### Memory Budget Guidelines

| Component | Memory Budget | Reason |
|-----------|---------------|--------|
| TAT Test | < 50 MB | Image loading + story input |
| WAT Test | < 30 MB | Rapid word display |
| Interview | < 100 MB | Audio + transcription |
| Study Materials | < 40 MB | Markdown rendering |
| Dashboard | < 20 MB | Overview data |

### Automated Monitoring

```kotlin
// Add to Application class for production monitoring
class SSBMaxApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Monitor memory usage in production
        if (!BuildConfig.DEBUG) {
            monitorMemoryUsage()
        }
    }

    private fun monitorMemoryUsage() {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        // Log memory warnings
        if (memoryInfo.lowMemory) {
            FirebaseCrashlytics.getInstance().recordException(
                Exception("Low memory warning: ${memoryInfo.availMem / 1024 / 1024} MB available")
            )
        }
    }
}
```

## 🧪 Testing Memory Management

### Unit Tests for Memory Safety

```kotlin
@Test
fun `ViewModel should not leak Activity reference after destruction`() {
    val activity = mockk<MainActivity>(relaxed = true)
    val viewModel = MyViewModel()

    // Set activity reference
    viewModel.setActivity(activity)

    // Clear ViewModel (simulate destruction)
    viewModel.onCleared()

    // Force garbage collection
    System.gc()
    System.runFinalization()

    // Activity should be garbage collected (no strong reference)
    // This is hard to test directly, but LeakCanary will catch it
}
```

### Integration Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class MemoryLeakTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun testNoLeaksDuringTestFlow() {
        // Start TAT test
        // Rotate device multiple times
        // Exit test
        // LeakCanary should not report leaks
    }
}
```

## 📋 Checklist for Memory Reviews

### Before Merging ViewModel Changes
- [ ] No direct Activity/Fragment references in ViewModel fields
- [ ] All listeners removed in `onCleared()`
- [ ] Coroutines use `viewModelScope` (not lifecycleScope)
- [ ] No Context references (use applicationContext if needed)
- [ ] Memory leak detection tests pass
- [ ] LeakCanary shows no new leaks

### Before Release
- [ ] All critical user flows tested with LeakCanary
- [ ] Memory Profiler shows no unexpected growth
- [ ] Heap dumps analyzed for retained objects
- [ ] Performance benchmarks meet memory budgets

## 🚨 Emergency Memory Issues

### If App is Crashing Due to OOM

1. **Immediate actions:**
   - Check LeakCanary for obvious leaks
   - Review recent ViewModel changes
   - Check bitmap loading (most common cause)

2. **Quick fixes:**
   - Add `android:largeHeap="true"` to manifest (temporary)
   - Reduce image cache sizes
   - Implement bitmap pooling

3. **Long-term fixes:**
   - Fix underlying memory leaks
   - Implement proper cleanup
   - Add memory monitoring

### Production Memory Monitoring

```kotlin
// Firebase Crashlytics custom metrics
FirebaseCrashlytics.getInstance().setCustomKey("memory_usage_mb",
    (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024
)

FirebaseCrashlytics.getInstance().setCustomKey("max_memory_mb",
    Runtime.getRuntime().maxMemory() / 1024 / 1024
)
```

## 📚 Resources

- [LeakCanary Documentation](https://square.github.io/leakcanary/)
- [Android Memory Profiler Guide](https://developer.android.com/studio/profile/memory-profiler)
- [Android Memory Management](https://developer.android.com/topic/performance/memory)
- [ViewModel Best Practices](https://developer.android.com/topic/libraries/architecture/viewmodel)

## 🎯 SSBMax Memory Goals

- **Zero memory leaks** in critical user flows (tests, interviews)
- **< 100 MB** memory usage during interview sessions
- **< 50 MB** memory usage during individual tests
- **Stable memory usage** (no continuous growth during app usage)

Regular memory profiling and LeakCanary monitoring ensure SSBMax provides a smooth, reliable experience for test preparation.
