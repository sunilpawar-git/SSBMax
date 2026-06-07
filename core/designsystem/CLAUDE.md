# core/designsystem/CLAUDE.md — Shared Components & Theming

**Scope:** Reusable Compose components, Material3 theming, design tokens, accessibility patterns. This file specializes [claude.md](../../claude.md) for the designsystem module—where UI consistency lives.

**Core Principle:** Components are building blocks: stateless, previewable, accessible. Theme is SSOT for colors/typography. Designers + developers collaborate here.

---

## Component API Design (Composables)

**Contract:**
```kotlin
@Composable
fun SSBCard(
  title: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  backgroundColor: Color = MaterialTheme.colorScheme.surface,
  onClick: (() -> Unit)? = null
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .clickable(enabled = enabled && onClick != null) { onClick?.invoke() },
    colors = CardDefaults.cardColors(containerColor = backgroundColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
    Text(
      title,
      modifier = Modifier.padding(16.dp),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

// ✅ ALWAYS include @Preview for each component
// ✅ Use MaterialTheme.* for colors/typography (never hardcode)
// ✅ Accept Modifier parameter (allows caller to customize layout)
// ✅ Provide sensible defaults (backgroundColor = MaterialTheme.colorScheme.surface)
// ✅ No business logic (no repository calls, no navigation)
```

**API Guidelines:**
- Include `modifier: Modifier = Modifier` as first optional parameter
- Use `MaterialTheme.*` for all design tokens (colors, typography, spacing)
- Provide `@Preview` composable for each component
- Document what each parameter does (KDoc)
- Test rendering (Compose test framework)

**Anti-patterns:**
- ❌ `Color.Red` or `Color(0xFFFF0000)` — use `MaterialTheme.colorScheme.error`
- ❌ Hardcoded `16.dp` padding — use `MaterialTheme.shapes`
- ❌ Business logic in component — pass data in, output UI only
- ❌ No @Preview — components should be visually testable

---

## Preview Composables (Mandatory)

**Pattern:**
```kotlin
@Composable
fun SSBCard(title: String, modifier: Modifier = Modifier) {
  // Component implementation
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun SSBCardPreview() {
  SSBMaxTheme {
    Column(modifier = Modifier.padding(16.dp)) {
      SSBCard(title = "Question 1")
      SSBCard(title = "Question 2")
    }
  }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SSBCardDarkPreview() {
  SSBMaxTheme(darkTheme = true) {
    SSBCard(title = "Dark Mode Question")
  }
}

// ✅ Preview with light + dark backgrounds
// ✅ Wrap in SSBMaxTheme for consistent styling
// ✅ Show realistic data
// ✅ Multiple previews for different states (enabled, disabled, etc.)
```

**Preview Helper (reusable):**
```kotlin
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CustomPreview(content: @Composable () -> Unit) {
  SSBMaxTheme {
    content()
  }
}

// Usage:
@Composable
fun SSBCardVariantsPreview() = CustomPreview {
  Column {
    SSBCard(title = "Default")
    SSBCard(title = "Disabled", enabled = false)
    SSBCard(title = "Large", modifier = Modifier.height(100.dp))
  }
}
```

---

## Material3 Theming (SSOT for Design Tokens)

**Theme Definition:**
```kotlin
// In designsystem/src/main/kotlin/com/ssbmax/core/designsystem/theme/SSBMaxTheme.kt
private val LightColorScheme = lightColorScheme(
  primary = SSBPrimary,
  secondary = SSBSecondary,
  tertiary = SSBTertiary,
  error = SSBError,
  background = SSBBackground,
  surface = SSBSurface,
  onPrimary = Color.White,
  onSecondary = Color.White,
  onBackground = Color.Black,
  onSurface = Color.Black
)

private val DarkColorScheme = darkColorScheme(
  primary = SSBPrimaryDark,
  secondary = SSBSecondaryDark,
  tertiary = SSBTertiaryDark,
  error = SSBErrorDark,
  background = SSBBackgroundDark,
  surface = SSBSurfaceDark
)

@Composable
fun SSBMaxTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
    dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }
  
  MaterialTheme(
    colorScheme = colorScheme,
    typography = SSBMaxTypography,
    shapes = SSBMaxShapes,
    content = content
  )
}

// ✅ All colors defined in one place (SSOT)
// ✅ Dark mode support built-in
// ✅ Dynamic theming via Material You (Android 12+)
// ✅ Used everywhere: SSBMaxTheme { /* app content */ }
```

**Typography:**
```kotlin
val SSBMaxTypography = Typography(
  displayLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp
  ),
  headlineMedium = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp
  )
)

// Usage in component:
Text("Question", style = MaterialTheme.typography.headlineMedium)
```

**Spacing (use `dp` directly, not hardcoded):**
```kotlin
val SmallPadding = 8.dp
val MediumPadding = 16.dp
val LargePadding = 24.dp

@Composable
fun MyComponent() {
  Column(modifier = Modifier.padding(MediumPadding)) {
    // ...
  }
}
```

---

## Accessibility (WCAG Guidelines)

**Content Descriptions (mandatory for all interactive elements):**
```kotlin
@Composable
fun TestCard(title: String, onTap: () -> Unit) {
  Card(
    modifier = Modifier
      .clickable(onTap)
      .semantics { 
        // ✅ Describe what tapping does
        contentDescription = "Take $title test"
      }
  ) {
    Text(title)
  }
}

// Screen readers will announce: "Take TAT test, button"
```

**Testing Tags (for UI testing):**
```kotlin
@Composable
fun SSBCard(title: String, testTag: String = "ssb-card") {
  Card(modifier = Modifier.testTag(testTag)) {
    Text(title, modifier = Modifier.testTag("$testTag-title"))
  }
}

// In tests:
composeTestRule.onNodeWithTag("ssb-card-title").assertTextEquals("My Title")
```

**Contrast + Font Size:**
- ✅ Contrast ratio ≥ 4.5:1 for normal text
- ✅ Contrast ratio ≥ 3:1 for large text (18sp+)
- ✅ Min font size 16sp for body text
- ✅ Links underlined or bold (not color alone)

**Dark Mode Support:**
```kotlin
@Composable
fun MyText(text: String) {
  Text(
    text,
    color = MaterialTheme.colorScheme.onSurface, // ✅ Auto-inverts for dark mode
    style = MaterialTheme.typography.bodyMedium
  )
}

// ❌ NEVER: Text(text, color = Color.Black)
```

---

## Component Library Organization

**Folder Structure:**
```
core/designsystem/
├── src/main/kotlin/com/ssbmax/core/designsystem/
│   ├── theme/
│   │   ├── Color.kt (color definitions)
│   │   ├── Typography.kt
│   │   ├── Shape.kt
│   │   └── SSBMaxTheme.kt (main theme)
│   ├── components/
│   │   ├── SSBCard.kt (component + preview)
│   │   ├── SSBButton.kt (component + preview)
│   │   ├── SSBTextField.kt (component + preview)
│   │   └── common/ (helpers like CustomPreview)
│   └── icon/ (custom icons if any)
```

**Naming Conventions:**
- Components: `SSB{ComponentName}.kt` (e.g., `SSBCard.kt`)
- Previews: In same file, `{ComponentName}Preview()` + `{ComponentName}DarkPreview()`
- Colors: descriptive (`SSBPrimary`, `SSBError`, `SSBBackground`)
- Spacing: `SmallPadding`, `MediumPadding`, `LargePadding`

---

## Component Testing (Compose Test Framework)

**Unit Test Template:**
```bash
./gradlew :core:designsystem:connectedDebugAndroidTest -k "SSBCardTest"
```

**Test Example:**
```kotlin
@RunWith(AndroidJUnit4::class)
class SSBCardTest {
  @get:Rule
  val composeTestRule = createComposeRule()
  
  @Test
  fun cardDisplaysTitleCorrectly() {
    composeTestRule.setContent {
      SSBMaxTheme {
        SSBCard(title = "Test Question")
      }
    }
    
    composeTestRule.onNodeWithTag("ssb-card-title")
      .assertTextEquals("Test Question")
  }
  
  @Test
  fun cardCallsOnClickWhenTapped() {
    var clicked = false
    composeTestRule.setContent {
      SSBMaxTheme {
        SSBCard(title = "Test", onClick = { clicked = true })
      }
    }
    
    composeTestRule.onNode(hasClickAction()).performClick()
    assertThat(clicked).isTrue()
  }
}
```

---

## Design System Evolution

**When to add a component:**
- ✅ Used in 2+ screens
- ✅ Non-trivial styling logic (complex state, animations)
- ✅ Design consistency required (brand component)

**When to keep it local:**
- ✅ One-off screen-specific component
- ✅ Simple (basic Text/Button styling)
- ✅ Not reused

**Versioning:**
- Components don't change their API (parameters remain consistent)
- New variants: create new components (e.g., `SSBCardSmall` instead of modifying `SSBCard`)
- Deprecation: mark old components `@Deprecated` with migration guide

---

## References

- **Root guidance:** [claude.md](../../claude.md) (quality limits, match conventions)
- **App usage:** [app/CLAUDE.md](../../app/CLAUDE.md) (Composable decomposition)
- **Theme colors:** MaterialTheme reference (Android docs)

---

**Last Updated:** June 2026 | **Maintainer:** Sunil Pawar
