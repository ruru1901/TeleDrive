# Scroll Performance Diagnosis

## Applied Optimizations ✅

1. **Removed expensive composition work** - Pre-calculated tint, cacheKey, sizeAndDate outside items lambda
2. **Single contentType** - Changed from mime-based to single "file" type for better recycling
3. **Increased thumbnail concurrency** - 8 parallel threads (was 4)
4. **LaunchedEffect for thumbnails** - Changed from SideEffect to LaunchedEffect
5. **Optimized derivedStateOf** - Skip filtering for Home tab

## Potential Remaining Issues

### 1. LazyColumnScrollbar Library
The `LazyColumnScrollbar` wrapper might add overhead. Test without it:

```kotlin
// Temporarily comment out LazyColumnScrollbar wrapper
LazyColumn(
    state = listState,
    // ... rest of config
)
```

### 2. Auto-refresh Every 15 Seconds
```kotlin
LaunchedEffect(onRefresh, state.busy) {
    while (true) {
        delay(autoRefreshIntervalMs)  // 15_000L
        if (!state.busy) onRefresh()
    }
}
```
This refreshes the entire file list every 15s, causing recomposition during scroll.

### 3. Coil AsyncImage Loading
AsyncImage for thumbnails might be slow. The optimization uses cached ImageBitmap but Coil still processes.

### 4. Complex Card Shapes
```kotlin
RoundedCornerShape(12.dp)  // on every item
```
Rounded corners require clipping which is expensive at scale.

### 5. Multiple State Observations
```kotlin
val sortOrder by driveViewModel.sortOrder.collectAsState()
val allSortedFiles by driveViewModel.sortedFiles.collectAsState()
```
Each state change triggers recomposition.

## Diagnostic Steps

### Step 1: Disable Auto-Refresh
Comment out the LaunchedEffect auto-refresh and test scrolling.

### Step 2: Simplify Item UI
Temporarily remove:
- Rounded corners
- Shadows/elevation
- Complex backgrounds

### Step 3: Check File Count
How many files are in the list? Performance degrades significantly above 500 items without proper optimization.

### Step 4: Profile with Android Studio
1. Open Android Studio Profiler
2. Start CPU recording
3. Scroll the list
4. Check for:
   - Composition time
   - Layout time
   - Drawing time

### Step 5: Enable Compose Metrics
Add to gradle:
```kotlin
kotlinOptions {
    freeCompilerArgs += listOf(
        "-P",
        "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
            project.buildDir.absolutePath + "/compose_metrics"
    )
}
```

## Quick Fixes to Try

### Fix 1: Disable Scrollbar Indicator
```kotlin
indicatorContent = null  // Remove the date bubble
```

### Fix 2: Increase Item Spacing
```kotlin
verticalArrangement = Arrangement.spacedBy(4.dp)  // was 2.dp
```
Less items visible = less composition work.

### Fix 3: Lazy Thumbnail Loading
Only load thumbnails for visible items (already implemented with LaunchedEffect).

### Fix 4: Disable Animations
```kotlin
// Remove AnimatedVisibility for notifications during scroll
```

## Expected Performance

- **< 100 files**: Buttery smooth 60fps
- **100-500 files**: Smooth with occasional frame drops
- **500+ files**: Needs pagination or virtual scrolling

## Test on Device

Run this command to see actual frame times:
```bash
adb shell dumpsys gfxinfo com.teledrive.android
```

Look for "Janky frames" count.
