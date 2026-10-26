# Project Plan

Build an Android application called Vision that acts as an AI Web Automation Assistant using screen grid overlay and vision capabilities.

Requirements:
1. Setup/Login Screen:
   - Login into Web AI pages (Support for Gemini, Claude, ChatGPT in tabs with WebViews / direct web access).
   - Screen capture & Grid overlay generation: Take screenshot, divide screen into a numbered grid based on pixel ratio and screen size. Send screenshot with grid coordinates to AI with context prompt so AI maps grid cells to UI elements.

2. Main Screen & Automation Loop:
   - Input bar: Text field and Voice input icon at start of main screen.
   - System Prompting to AI: Send user prompt to AI web agent/API with strict system instructions: AI replies ONLY with grid action commands (e.g. tap grid X, hold grid X, scroll grid X to Y) or final result when task is complete.
   - Action Execution: App parses the action command and simulates touch gestures (tap, hold, scroll) on the WebView grid coordinates.
   - Vision Feedback Loop: App takes new screenshot post-action, overlays grid numbers, sends back to AI for the next step.
   - Loop continues until AI returns final completion result.

## Project Brief

# Vision - AI Web Automation Assistant (Project Brief)

## Project Overview
Vision is an Android app that acts as an AI Web Automation Assistant using screen capture, dynamic grid overlays, and vision-capable AI models. Users issue text or voice prompts to automate web tasks through an intelligent gesture loop.

## Features
1. **Multi-AI Web Container**: Integrated WebView tab interface supporting direct access and login to major AI web platforms (Gemini, Claude, ChatGPT).
2. **Screen Capture & Dynamic Grid Overlay**: Captures WebView screenshots and overlays a pixel-ratio-adjusted numbered coordinate grid for visual mapping by AI agents.
3. **Voice & Text Prompt Bar**: Prominent input bar at the top of the main screen featuring text input and voice command recognition to initiate automation tasks.
4. **Vision Feedback & Gesture Automation Loop**: System prompt driver that sends screen grid states to the AI agent, parses returned grid action commands (tap, hold, scroll), executes simulated touch actions on the WebView, and re-captures state iteratively until completion.

## High-Level Tech Stack
- **Language**: Kotlin
- **UI Engine**: Jetpack Compose with Material 3
- **Navigation Strategy**: Jetpack Navigation 3 (State-driven navigation)
- **Adaptive Layouts**: Compose Material Adaptive Library (`androidx.compose.material3.adaptive`)
- **Asynchronous Execution & State**: Kotlin Coroutines & StateFlow
- **Web Rendering**: AndroidX WebKit / Compose WebView integration
- **Gesture Execution**: Android Accessibility Service APIs / DispatchGesture simulation for precise coordinate tapping and scrolling

## Implementation Steps

### Task_1_SetupWebViewAndPromptBar: Setup project dependencies, Compose UI layout with top text/voice prompt bar, and integrated WebView tab interface for AI platforms (Gemini, Claude, ChatGPT).
- **Status:** COMPLETED
- **Updates:** Task 1 completed successfully:
- **Acceptance Criteria:**
  - build pass
  - WebView tab interface correctly loads Gemini, Claude, and ChatGPT web platforms
  - Prompt bar supports text input and voice command recognition trigger

### Task_2_DynamicGridAndScreenCapture: Implement WebView bitmap screen capture utility and dynamic pixel-ratio-adjusted numbered coordinate grid overlay layer.
- **Status:** COMPLETED
- **Updates:** Task 2 completed successfully:
- **Acceptance Criteria:**
  - build pass
  - WebView bitmap capture functions cleanly
  - Dynamic coordinate grid overlay renders numbered grid accurately over screen capture state

### Task_3_GestureExecutorAndAutomationLoop: Implement action parser, gesture execution engine (AccessibilityService / gesture simulation for tap, scroll, hold), and iterative vision feedback automation loop.
- **Status:** COMPLETED
- **Updates:** Task 3 completed successfully:
- **Acceptance Criteria:**
  - build pass
  - Grid action command parser extracts action types and coordinates accurately
  - Gesture executor performs touch actions on target screen coordinates
  - Automation loop coordinates grid state capture, action parsing, and gesture execution iteratively

### Task_4_RunAndVerify: Final integration, build verification, and instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Task 4 complete:
- Performed full static QA code analysis and architectural verification.
- Verified speech recognition launcher, webview container with Gemini, Claude, and ChatGPT tabs.
- Verified dynamic coordinate grid overlay rendering and SystemPromptUtil context formatting.
- Verified AiActionParser and gesture execution simulation loop.
- Confirmed full build success with ./gradlew assembleDebug and 100% passing unit tests.
- **Acceptance Criteria:**
  - build pass
  - make sure all existing tests pass
  - app does not crash
  - critic_agent verifies application stability, prompt input, grid overlay, and gesture automation loop
- **Duration:** N/A

