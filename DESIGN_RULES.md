# Notely — Permanent UI/UX, Design System & Safe Modification Rules

## ⚠️ ADDENDUM (Required reading before applying any section below)

1. **Section 8 (Base Colors):** Before applying ANY color value from this document, the agent MUST first inspect the CURRENT `app/src/main/res/values/attrs.xml`, `colors.xml`, and `themes.xml`. If a conflict exists between this document's baseline colors and the already-implemented theming system, STOP and report the conflict explicitly — do not silently overwrite working values.

2. **Section 19 (Feature-folder restructuring):** This is a FUTURE direction only. The current package structure (`ui/`, `data/`, `model/`, `util/`) must be preserved as-is until the project genuinely outgrows it (e.g. when adding Search, Tags, or another major feature per Section 32). Do not restructure packages for the current MVP-sized codebase.

3. **Suggest-Only Tooling Rule:** If, while working on any task, the agent identifies a library, tool, or dependency that would genuinely help, it must NOT install or add it automatically. Instead, it must list the suggestion explicitly in a dedicated "Suggestions" section of its final report, with a one-line justification, and wait for explicit approval before acting on it.

---

## 1. Purpose

These rules are permanent project rules for all future OpenCode/Agent work on Notely.

The goal is to make the application:

- visually consistent;
- easy to modify;
- easy to extend with new features;
- safe to maintain from a mobile/Termux environment;
- resistant to regressions;
- reusable instead of duplicated;
- independent of accidental UI layout changes.

The agent MUST treat these rules as architectural constraints, not optional design suggestions.

---

## 2. Core Principle

Separate UI structure, reusable UI components, design tokens, screen behavior, business logic, and data access.

A visual change must not require changing unrelated application logic.

A behavior change must not require redesigning unrelated screens.

A new feature must not require rewriting existing screens unless there is a concrete architectural reason.

The application should be structured so that:

Design Tokens
      ↓
Reusable UI Components
      ↓
Screens
      ↓
Features / Presentation Logic
      ↓
Domain / Repository
      ↓
Storage / External Systems

Dependencies should flow downward.

Lower-level data/storage code must never depend on a specific screen position, button location, or visual styling.

---

## 3. Screen Separation

Each major application screen MUST have a clearly defined implementation boundary.

For example:

MainScreen
SettingsScreen
NoteEditorScreen

A screen is responsible for:

- layout;
- composition of UI components;
- screen-specific presentation behavior;
- navigation events;
- connecting UI events to the appropriate application layer.

A screen MUST NOT become a dumping ground for:

- reusable button styling;
- global colors;
- global typography;
- storage implementation;
- duplicated business logic;
- unrelated feature logic.

If a piece of UI or behavior is reused by multiple screens, evaluate whether it belongs in a reusable component or shared layer.

---

## 4. Reusable UI Components

Do NOT create a separate file for every individual button or text element.

Instead, create reusable components based on actual UI roles.

Examples:

PrimaryButton
SecondaryButton
TextButton
IconButton
AppFab
AppTopBar
AppTextField
AppRadioOption
NoteCard
EmptyState
LoadingState
ErrorState

If five buttons share the same design and behavior, there should normally be one reusable component/style rather than five duplicated implementations.

The objective is:

One design rule
      ↓
One reusable component
      ↓
Many screens can use it

This allows a global visual change to be made safely in one place.

---

## 5. Component Independence From Position

A reusable component MUST NOT depend on where it happens to be placed on a screen.

For example:

- A button must work whether it is at the top, middle, or bottom.
- A card must work inside a list or another valid container.
- A text field must not assume a specific screen width.
- An icon must not assume RTL or LTR positioning through hardcoded coordinates.
- Navigation behavior must not depend on the visual position of a view.

Moving a button from:

Bottom

to:

Top

must normally be a layout/composition change only.

It must NOT require changing:

- repository logic;
- storage logic;
- note models;
- business rules;
- unrelated screens;
- persistence;
- authentication/security behavior;
- navigation architecture unless the navigation destination itself changes.

---

## 6. UI Must Be Declarative in Structure, Not Behaviorally Coupled

The UI defines:

What the user sees
Where it appears
How it looks
What user interaction occurred

The underlying application layers define:

What the action actually does
How data is validated
How data is stored
How errors are handled

For example:

Save Button
    ↓
UI event
    ↓
Presentation/Application logic
    ↓
NoteRepository
    ↓
LocalJsonStorage

The Save Button MUST NOT directly implement file-storage rules merely because it is the visible Save button.

---

## 7. Centralized Design System

All global visual values MUST be centralized whenever practical.

Do not scatter hardcoded visual values throughout unrelated files.

Create a clear design-token/theme layer for values such as:

Colors
Typography
Dimensions
Spacing
Shapes
Elevation
Component styles
Theme behavior

Example conceptual structure:

theme/
 Colors
 Typography
 Dimensions
 Spacing
 Shapes
 Theme

The exact implementation may follow the existing Android Java/XML architecture.

Do NOT introduce a new UI framework merely to implement this rule.

---

## 8. Notely Base Design Tokens

The current Notely visual direction should use these values as the baseline unless a future design decision explicitly changes them.

Colors

Primary:          #2B5E89
Primary Pressed:  #234E72
Background:       #F7F8FA
Surface / Note:   #FFFFFF
Primary Text:     #20242A
Secondary Text:   #68727E
Divider / Border: #E2E5E8
Disabled:         #AEB5BC
Error:            #C84646

Do not introduce additional colors casually.

If a new color is genuinely required, first determine whether an existing semantic color can be reused.

---

## 9. Semantic Colors

UI components should consume semantic roles rather than knowing where a color came from.

Prefer concepts such as:

colorPrimary
colorBackground
colorSurface
colorTextPrimary
colorTextSecondary
colorDivider
colorError

rather than repeatedly embedding raw color values inside individual screens.

This is especially important for Light/Dark/System themes.

A future theme change should not require manually searching dozens of files.

---

## 10. Spacing System

Use a consistent spacing scale:

4dp
8dp
12dp
16dp
20dp
24dp
32dp
40dp
48dp
64dp

Avoid arbitrary values such as:

17dp
23dp
37dp
43dp

unless there is a documented reason.

Spacing should be consistent across:

- screens;
- cards;
- buttons;
- text fields;
- settings sections;
- empty states;
- lists;
- dialogs.

---

## 11. General Component Standards

Baseline standards:

Main screen horizontal padding: 24dp
Standard icon size:             24dp
FAB size:                       56dp
Primary button height:          56dp
Primary button radius:          28dp
Default card radius:            16dp

Typography baseline:

Large screen title: 24–28sp
Body text:          16–18sp
Secondary text:     14–16sp
Metadata:           12–14sp

These are baseline values, not permission to blindly force every element into the same size.

Visual hierarchy must remain appropriate to the element's semantic importance.

---

## 12. Notes

The default Note surface should remain visually calm.

Baseline:

Background: #FFFFFF
Border:     #E2E5E8
Radius:     16dp
Padding:    16dp

Do not introduce multiple bright note colors without a product requirement.

If colored notes are introduced later, they must be implemented as a controlled theme/token system rather than arbitrary per-screen colors.

---

## 13. App Bar / Navigation UI

The application should use a consistent top-bar pattern.

Examples:

Main:
[Settings]                         [Notely]

Settings:
[Back]                             [Settings]

The exact visual arrangement must correctly follow RTL/LTR.

Do not hardcode physical left/right assumptions where Android's RTL-aware layout system can be used.

The same top-bar component/style should be reused whenever the screen semantics are equivalent.

---

## 14. RTL and Localization

Arabic and English are first-class languages.

The default language should follow the device/system language when no explicit user preference has been selected.

The UI MUST support:

Arabic
English
RTL
LTR

Do not create separate duplicated screen implementations merely because the language direction changes.

Use Android's localization and RTL mechanisms correctly.

Text must come from resources rather than being hardcoded inside Java/UI code.

A visual modification MUST NOT break:

- Arabic text;
- English text;
- RTL layout;
- LTR layout;
- text alignment;
- icon direction;
- navigation direction.

---

## 15. Theme Independence

Light and Dark themes must share the same structural layout.

Changing theme should primarily change:

colors
surfaces
contrast
elevation treatment

It should not randomly change:

navigation
component hierarchy
data behavior
screen logic

Theme-specific values should be centralized.

Avoid hardcoded light-theme colors inside individual components.

---

## 16. Safe UI Modification Rule

Before changing any UI, the agent MUST:

1. Inspect the current repository.
2. Identify the screen being changed.
3. Identify reusable components used by that screen.
4. Identify shared theme/design tokens.
5. Identify any behavior attached to the UI element.
6. Determine whether the requested change is visual, behavioral, architectural, or a combination.
7. Change the smallest appropriate layer.

Do NOT immediately rewrite the entire screen.

Do NOT replace working architecture simply because another implementation appears cleaner.

---

## 17. Moving an Element

If the task is:

Move a button.»

The default change should be:

Screen layout/composition only.

It should NOT automatically become:

Rewrite button
Rewrite repository
Rewrite storage
Rewrite navigation
Rewrite model

If moving the button genuinely changes navigation or interaction semantics, then only the affected behavior should be changed.

The agent MUST explicitly identify that dependency before modifying it.

---

## 18. Changing a Component

If the task is:

Change the appearance of all primary buttons.»

The preferred approach is:

Modify PrimaryButton / shared style / design token.

Do NOT manually modify every screen unless the existing architecture proves that the buttons are not actually shared.

If the task is:

Change only one screen's button.»

Reuse the existing component and expose/configure only the necessary variation.

Do not create a duplicate component unless the variation represents a real reusable semantic role.

---

## 19. Adding a New Feature

New features should be isolated from unrelated features.

Conceptually:

features/
 notes/
 settings/
 search/
 favorites/
 ...

The exact directory structure must follow the existing Java/XML architecture.

A new feature should reuse:

Theme
Design Tokens
Shared Components
Navigation Infrastructure
Repository Interfaces
Validation
Error Handling

It should not copy and modify existing components unnecessarily.

---

## 20. Preventing Regression

Every modification must preserve existing functionality unless the task explicitly requests a behavior change.

The agent MUST consider:

Create note
Edit note
Delete note
Save note
Settings
Language switching
Theme switching
Navigation
Persistence
Empty state
Error handling

when changing shared UI.

If a shared component is modified, inspect all known usages before changing its contract.

---

## 21. Component API Stability

Reusable components should expose clear, minimal interfaces.

Do not make screens depend on internal implementation details of a component.

For example, a screen should conceptually care about:

onClick
text
enabled

rather than:

internal padding
internal drawable
internal color implementation

This allows visual implementation to change without forcing every screen to change.

---

## 22. No Unnecessary Duplication

Before creating a new component, search for an existing equivalent.

Before creating a new style, search for an existing style.

Before creating a new color, search the theme.

Before creating a new spacing value, check the spacing system.

Before creating a new utility, search the existing codebase.

Reuse first.

Create new abstractions only when they provide real value.

---

## 23. UX Rules

Visual design is not only about colors.

The agent must consider:

- hierarchy;
- readability;
- touch target size;
- spacing;
- alignment;
- consistency;
- feedback;
- loading states;
- empty states;
- error states;
- disabled states;
- destructive-action confirmation;
- accessibility;
- Arabic/English readability;
- Light/Dark contrast.

Do not optimize for visual appearance at the expense of usability.

---

## 24. Touch Targets

Interactive elements must remain comfortably usable on a phone.

Do not make controls visually small merely to make the UI look minimal.

Spacing between interactive elements must prevent accidental taps.

A design change must preserve practical touchability.

---

## 25. Responsive Layout

Do not rely on fixed screen coordinates.

The UI must adapt to different:

screen widths
screen heights
font sizes
Arabic/English text lengths
orientation where supported

Use Android layout constraints/weights/wrap-content/match-constraints and other appropriate responsive mechanisms instead of positioning elements using arbitrary coordinates.

---

## 26. Accessibility

Do not sacrifice accessibility for visual simplicity.

Interactive controls should have:

- meaningful labels;
- appropriate content descriptions where required;
- sufficient contrast;
- usable touch targets;
- readable text;
- sensible focus/navigation behavior.

Decorative elements should not be incorrectly exposed as interactive controls.

---

## 27. Separation of Visual and Functional Changes

Every task should first be classified:

VISUAL
BEHAVIOR
DATA
ARCHITECTURE
SECURITY
BUILD/CI

If the task is VISUAL:

Do not modify DATA unless absolutely required.

If the task is DATA:

Do not redesign the UI unless explicitly required.

If the task is BUILD/CI:

Do not modify application architecture unless absolutely required.

This prevents unrelated regressions.

---

## 28. Change Impact Analysis

Before modifying a shared component or common resource, determine:

Who uses it?
What screens depend on it?
Does it affect Arabic?
Does it affect English?
Does it affect RTL?
Does it affect LTR?
Does it affect Light theme?
Does it affect Dark theme?
Does it affect interaction behavior?

For high-impact changes, inspect all usages before editing.

---

## 29. Validation After UI Changes

After meaningful UI changes, the agent should validate as much as the available environment permits.

At minimum:

XML/resource validation
Unit tests where applicable
Git diff inspection
Git status inspection
CI build when required

Do not claim runtime validation unless the application was actually run and tested.

Do not claim APK generation unless a real APK was generated and verified.

The project already follows the principle that successful builds and artifacts must be based on actual evidence rather than assumptions.

---

## 30. No Local-Build Assumptions

The Android project currently uses GitHub Actions as the authoritative reproducible build path.

Do not introduce local-build dependencies into the application architecture.

Machine-specific workarounds must remain isolated from the product source whenever possible.

The existing project documentation explicitly prioritizes preserving the working architecture and using GitHub Actions as the reproducible build pipeline.

---

## 31. No Architecture Rewrite for UI Modernization

Modernizing the UI does NOT justify:

- replacing the whole application;
- migrating frameworks without need;
- replacing Java/XML merely for appearance;
- introducing a large dependency tree;
- rewriting working persistence;
- rewriting repository layers;
- replacing navigation infrastructure without evidence.

The project direction is incremental modernization while preserving working architecture.

---

## 32. Feature Growth Rule

Notely is expected to grow over time.

Future additions may include things such as:

Search
Favorites
Archive
Trash
Tags
Reminders
Attachments

These must be added incrementally.

A new feature should integrate with the existing design system rather than introducing a new visual language.

The agent must ask:

Can this feature reuse an existing component, token, repository boundary, or navigation pattern?»

before creating something new.

---

## 33. User-Friendly Modification Model

The project should remain understandable enough that a developer working from a phone can make targeted changes.

Examples:

Change primary color
        ↓
Theme / Colors

Change all primary buttons
        ↓
PrimaryButton / shared style

Change settings screen layout
        ↓
SettingsScreen

Change note card appearance
        ↓
NoteCard / item style

Change typography
        ↓
Typography / theme

Add Search feature
        ↓
Search feature boundary

The exact paths must reflect the real repository structure; these examples define the architectural responsibility, not mandatory filenames.

---

## 34. Agent Decision Rule

When receiving a modification request, the agent MUST NOT immediately edit files.

First determine:

What is the requested outcome?
Which layer owns this responsibility?
Is an existing component reusable?
What is the smallest safe change?
What other code depends on it?
How will the change be validated?

Then implement only the required change.

---

## 35. Forbidden Patterns

Avoid:

Duplicate buttons with nearly identical styles
Duplicate screens with slightly different code
Hardcoded colors everywhere
Hardcoded spacing everywhere
Hardcoded screen coordinates
Business logic inside visual components
Storage logic inside UI elements
Feature logic copied between screens
Unnecessary dependencies
Large refactors for small UI changes
Framework migrations without justification
Changing unrelated files
Removing working functionality

---

## 36. Final Rule

The most important rule is:

Make every part of Notely independently changeable where practical, reusable where appropriate, and isolated enough that changing one concern does not unnecessarily break another.»

A UI element's position is a presentation concern.

A button's appearance is a design-system concern.

A button's action is a behavior concern.

A note's persistence is a data concern.

These concerns must remain separated.

Therefore:

Move a button
    ≠
Rewrite its behavior

Change a button color
    ≠
Rewrite the screen

Change screen layout
    ≠
Rewrite persistence

Add a feature
    ≠
Rewrite existing features

Modernize UI
    ≠
Rewrite the architecture

This principle should guide every future OpenCode task for Notely.

---

## 37. Priority Order

When requirements conflict, prioritize:

1. Correctness
2. Security
3. Existing functionality
4. Data integrity
5. Architecture stability
6. Accessibility / usability
7. Design consistency
8. Performance
9. Visual polish

Never sacrifice correctness, security, data integrity, or existing functionality merely for visual appearance.

---

## 38. Required Agent Behavior

For every future Notely modification:

AUDIT
  ↓
CLASSIFY THE CHANGE
  ↓
IDENTIFY THE RESPONSIBLE LAYER
  ↓
CHECK EXISTING COMPONENTS
  ↓
CHECK DEPENDENCIES / USAGES
  ↓
MAKE THE SMALLEST SAFE CHANGE
  ↓
VALIDATE
  ↓
INSPECT DIFF
  ↓
BUILD / CI WHEN APPLICABLE
  ↓
REPORT VERIFIED FACTS ONLY

The agent must not confuse:

"I changed the code"

with:

"The change was validated successfully."

Only actual validation evidence may be reported as successful.
