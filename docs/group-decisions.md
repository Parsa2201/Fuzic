# Group Decisions Skill

Use this document as the project-specific decision guide for Fuzic. It is written for AI agents and human contributors working on the Android app.

## Purpose

This guide captures team decisions that must be followed while planning, coding, reviewing, and documenting the project. It complements the Android Skills guidance and the project spec.

If this guide conflicts with the Android Skills, stop and ask the developer which rule to follow. Do not resolve conflicts independently.

## Required Setup

- Install the Android Skills before using AI agents for project coding.
- Ensure AI agents can access the project documentation before making code changes.
- Ensure the Eraser MCP server is available and authorized before asking AI agents to update diagrams.
- Do not use Eraser AI credits. Create or update diagrams only through MCP APIs that do not consume AI tokens.

## Core Decisions

- Dependency injection: use Hilt.
- Image loading: use Coil.
- Navigation routes must be type-safe. Do not use raw string routes. Define routes with classes and objects following Google recommendations.
- Commits must follow Conventional Commits.
- Follow SOLID and DRY principles.
- Keep features loosely coupled so team members can work in parallel.
- When adding a feature, all tests must pass before the feature is considered complete.
- If an implementation needs missing assets, notify the developer and ask for the assets.
- Discuss and approve the color palette before defining colors and themes.

## Project Structure

Use the following package and folder structure unless a new folder is explicitly agreed on.

```text
.
├── ui/
│   ├── navigation/
│   │   └── AppNavGraph.kt
│   ├── theme/
│   ├── screens/
│   └── components/
├── repository/
├── model/
├── domain/ (optional)
├── di/
├── data/
│   ├── settings/
│   ├── serialization/ (optional)
│   ├── local/
│   │   ├── entity/
│   │   ├── dao/
│   │   ├── database/
│   │   └── converter/ (optional)
│   └── remote/
└── util/ (optional)
```

### Folder Responsibilities

- `ui/`: UI-related files, navigation, themes, and ViewModels.
- `ui/navigation/`: app navigation graph, including `AppNavGraph.kt`.
- `ui/theme/`: colors, typography, shapes, and app theme.
- `ui/screens/`: one folder per screen. Each screen folder contains its composables and ViewModel.
- `ui/components/`: reusable composables used by more than one screen. Group related components into their own files.
- `repository/`: repository interfaces and implementations.
- `model/`: UI-facing models such as data classes, enum classes, data objects, and similar model types.
- `domain/` (optional): business logic and use cases that sit between UI and data when that extra layer is useful.
- `di/`: Hilt dependency injection modules.
- `data/`: data-related files.
- `data/settings/`: app settings storage and restoration.
- `data/serialization/` (optional): serialization helpers, such as a `JsonProvider` if needed.
- `data/local/`: local database files.
- `data/local/entity/`: database entities and tables.
- `data/local/dao/`: DAOs.
- `data/local/database/`: app database definitions.
- `data/local/converter/` (optional): converters for SQLite-compatible values and back.
- `data/remote/`: networking files.
- `util/` (optional): truly shared generic helpers.

If a UI file does not clearly fit the existing categories, discuss whether it belongs directly under `ui/` or in a new/existing subfolder.

Use `domain/` only when it removes real complexity. Good domain candidates include use cases that combine multiple repositories, enforce product rules, or contain business logic reused by multiple ViewModels. Examples include checking whether a user can download a song, validating playlist names, building a home feed from multiple sources, or applying premium/playback rules.

Do not create domain use cases that only call one repository method with no extra logic. Simple calls can stay in the ViewModel or repository. The domain layer is optional, so add it feature by feature when it is really necessary.

Avoid adding to `util/` unless the helper is generic and truly shared. Overusing this folder increases coupling.

## Adding New Folders

Add new folders or subfolders only when the existing structure does not fit. New structure decisions should be documented here or in the relevant project documentation.

## UI Rules

- Break large UI components into smaller subcomponents when it improves reuse, readability, and testing.
- Each screen must have a top-level composable used by navigation.
- The navigation-level top-level composable does not need a preview.
- ViewModels are passed to the navigation-level composable only.
- The navigation-level composable extracts state and callbacks from the ViewModel, then passes plain state and functions into the screen's main composable.

## Compose Preview Rules

Every reusable or screen-level UI composable should have previews that help developers inspect behavior quickly.

Required preview coverage:

1. Provide sample models for composables that require model input.
2. Include an empty-state preview when the composable can render with no content.
3. Include delayed-loading previews for shimmer effects. These previews are intended for emulator testing.
4. Do not pass empty lambdas when interaction matters. Preview callbacks should demonstrate realistic sample behavior.
5. Include previews for every meaningful component state.
6. Prefer one reusable preview composable when it can generate multiple previews cleanly.
7. Include English and Persian previews. Use English for the default preview. Use Persian for the default and all additional state previews.
8. Group previews by composable so they are easy to navigate.
9. Make animations and subcomponent interactions visible and testable in previews.

## Testing Rules

- Write tests for non-UI components to verify behavior.
- Use networking test tools or packages that simulate network behavior.
- Test networking delay and network error scenarios.
- After adding a feature, run the relevant test suite and make sure all tests pass.

## Branching Workflow

- Develop each feature in its own branch.
- Develop subfeatures in their own branches when useful.
- Merge subfeature branches into their parent feature branch.
- Merge completed feature branches into `master`.
- UI-specific and data-specific feature work may happen in parallel.

## Planning Workflow

Before coding, create a top-level plan that defines the steps required.

The plan should:

- Start by defining all project criteria.
- Identify feature dependencies.
- Make clear which tasks are assigned to a specific person and which tasks are shared phases.
- Keep dependencies between features as small as possible.

## Feature Dependency Graph

Maintain a feature graph that shows:

- All major features.
- Dependencies between features.
- Shared dependencies used by multiple features.
- Independent features that can be developed in parallel.

Many UI screens should not depend on each other directly. If multiple screens depend on a shared capability, model that shared dependency explicitly.

## Diagrams And Eraser

All project diagrams must be maintained in Eraser.

- Workspace/team: Parsa's Team 2.
- Project name: Android Project.
- AI agents access the diagrams through the Eraser MCP. Before reading or editing diagrams, the MCP must be available and authorized, and the agent must locate the project file by calling `list_files` on Parsa's Team 2 and selecting the file titled `Android Project` (search-by-title alone may miss it).
- Use diagram-as-code when creating architecture diagrams, process diagrams, feature graphs, or ERDs.
- Arrange diagram component positions so the final diagram is clean and readable when the API allows positioning.
- Show all processes and graphs in Eraser.
- Show all ERDs in Eraser.

Diagrams are for human understanding and may change over time. They are not guaranteed to be fully correct or final.

## Phase 1 Responsibilities

The split mirrors the "Phase 1" BPMN diagram in the Android Project Eraser file.

### Parsa

- Set up the GitHub project.
- Define the app UI.
- Integrate the chosen backend client in the Android app (the Phase 1 BPMN's "Use Alternative Backend in Android App" branch — Firebase was evaluated and rejected).

### Bagher

- Own the backend. This includes evaluating the candidate (Firebase vs alternative), defining the App–Backend API, and developing the chosen backend.
- For Fuzic, the chosen backend is **Supabase**. Bagher's deliverable is a self-hosted Supabase project with schema, auth, storage, and realtime ready for the Android client.

### Sina

- Prepare AI tooling.
- Learn media playback well enough to define the player service.
- Exit event: Media Playback Ready.

## Known Product Planning Notes

- The chat feature belongs in the final phases.

## Conflict Handling

Before coding, compare this document with the Android Skills and identify possible conflicts.

When a conflict exists:

- Stop before making the affected change.
- Explain the conflict clearly.
- Ask the developer which rule to follow.
- Continue only after the developer chooses.

## Agent Checklist

Before implementing a feature:

- Read this document.
- Read the project spec.
- Check the Android Skills guidance.
- Confirm no decision conflicts apply.
- Create or update the implementation plan.
- Identify missing assets and request them before coding.
- Identify required tests and previews.

During implementation:

- Follow the approved folder structure.
- Use Hilt, Coil, type-safe navigation, and project theme rules.
- Keep features loosely coupled.
- Add previews for UI components.
- Add tests for non-UI behavior and network edge cases.

Before finishing:

- Run relevant tests.
- Confirm previews cover required states.
- Confirm new documentation or diagrams are updated when needed.
- Use Conventional Commits for any commit.
