# TaskFlow — Mobile Task Management Application

A modern, minimal, production-quality native Android application and Node.js REST API backend built for the EnvisionStudio Mobile App Developer Practical Assessment.

---

## 1. How to Run Locally

### Selected Technology Stack
- **Android Application**: Native Android with Kotlin 2.0.21, MVVM Architecture, Clean Architecture principles, Jetpack Navigation Component, Material 3 Design, ViewBinding, Kotlin Coroutines, StateFlow, Retrofit 2.11.0, OkHttp 4.12.0, Gson, and Hilt (Dagger) 2.51.1 for dependency injection.
- **Backend API**: Node.js (v18+) with Express.js, SQLite3 for persistent relational data storage, RESTful endpoint architecture, and centralized error handling.

### Why the Stack Was Chosen
- **Native Kotlin + MVVM & Clean Architecture**: Ensures clean separation of concerns, high maintainability, testability, lifecycle-safe StateFlow collection (`repeatOnLifecycle`), and compile-time dependency injection via Hilt without the overhead of experimental dependencies.
- **XML Layouts with Material 3**: Provides high-performance, polished, predictable UI components (cards, chips, buttons, progress indicators) adhering to commercial design standards.
- **Node.js + Express + SQLite**: Delivers a lightweight, zero-configuration REST API with genuine file-backed SQL persistence, predictable ACID transactions, and fast cold-start performance, perfectly matching the assessment requirements without requiring external database servers.

### Prerequisites
- **Node.js**: v18.0.0 or higher (verified on Node v22.23.4) and npm.
- **Android Development Environment**: Android Studio (Ladybug, Koala, Hedgehog or newer) with Android SDK 35 (minSdk 26).
- **JDK**: Java Development Kit 17 or 21 (bundled with Android Studio JBR).
- **Physical Device**: Any Android phone connected to the **same Wi-Fi network** as your development PC (or Android Virtual Emulator).

---

### Backend Setup & Commands

1. Open a terminal and navigate to the `backend` directory:
   ```bash
   cd backend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run the automated backend test suite (16 tests verifying CRUD, filtering, edge cases, and persistence):
   ```bash
   npm test
   ```
4. Start the backend REST API:
   ```bash
   npm start
   ```
   The backend server will bind to `0.0.0.0:3000`, making it accessible on:
   - **Localhost (PC browser)**: `http://localhost:3000/`
   - **Same Wi-Fi Network**: `http://192.168.0.110:3000/`
   - **Android Emulator**: `http://10.0.2.2:3000/`
   
   On first run, it automatically seeds the exact 15 assessment tasks into SQLite at `backend/database/taskflow.db`.

---

### Android Setup & Running on Same Wi-Fi Device

1. **Verify Your PC's Wi-Fi IP**:
   - Check your computer's Wi-Fi IPv4 address using `ipconfig` (e.g. `192.168.0.110`).
   - The app is pre-configured with `server.ip=192.168.0.110` in `android/local.properties`. If your Wi-Fi IP changes, simply update `server.ip` in `android/local.properties`.
2. **Build the Debug APK**:
   ```powershell
   cd android
   .\gradlew.bat assembleDebug
   ```
   The output APK is generated at:
   `android/app/build/outputs/apk/debug/app-debug.apk`
3. **Install and Run on Your Phone**:
   - **Option A (Via USB Cable Once)**:
     Connect your phone via USB cable with USB Debugging enabled:
     ```powershell
     cd android
     .\gradlew.bat installDebug
     adb shell am start -n com.taskflow/.MainActivity
     ```
     Once installed, you can disconnect the USB cable! The app connects directly to `http://192.168.0.110:3000/` over Wi-Fi.
   - **Option B (Direct APK Transfer)**:
     Transfer `android/app/build/outputs/apk/debug/app-debug.apk` to your phone via Google Drive, WhatsApp, or file transfer, and tap Install. Ensure your phone is connected to the same Wi-Fi router.
   - **Option C (Android Studio)**:
     Open the `android` folder in Android Studio, select your phone from the device dropdown, and click **Run 'app'** (`Shift + F10`).

---

### Environment & Network Configuration

- **Same Wi-Fi Network Communication**: The app connects directly to your PC's IP `http://192.168.0.110:3000/` across the local Wi-Fi router.
- **Dynamic IP Configuration**: You can change the target backend IP at any time without editing Kotlin code by updating `server.ip=<your_ip>` in `android/local.properties`.
- **Dual Fallback Support**: If the phone is connected via USB cable with `adb reverse tcp:3000 tcp:3000`, the app automatically falls back to `http://127.0.0.1:3000/` if Wi-Fi becomes unavailable.
- **Virtual Emulator Support**: If run inside an emulator, the app automatically detects the virtual environment and uses `http://10.0.2.2:3000/`.
- **Cleartext Traffic**: Network security configuration (`res/xml/network_security_config.xml`) explicitly permits HTTP traffic on private network ranges (`192.168.*.*`, `10.*.*.*`, and `localhost`).

---

## 2. Screen Structure

```text
Task List → Task Detail → Back to Task List
```

### Add Task Navigation Flow
```text
Task List → Add Task Form → Task List (automatic pop-back with immediate list & summary refresh)
```
- From the **Task List** screen, the user taps either the top app bar "+ Add Task" button or the bottom Floating Action Button to navigate to the **Add Task** form (`AddTaskFragment`).
- On the Add Task form, the user fills in the title, optional description, optional due date (via Material Date Picker), priority chips, and assignee chips.
- Upon clicking **Create Task**, the client validates required fields and submits a `POST /api/tasks` request to the backend.
- Once created, the screen automatically navigates back (`navigateUp()`), and the Task List screen refreshes its underlying task list and summary counters while **preserving the user's previously selected filters**.

---

## 3. Decision 1 — Optimistic vs. Pessimistic

We chose an optimistic update strategy for task completion toggles so mobile users receive instantaneous visual feedback without network latency, with the state reliably rolled back and an error snackbar displayed if the server request fails.

---

## 4. Decision 2 — Filter Persistence

Filters are preserved when navigating from the Task List to Task Detail and returning so users can systematically inspect and manage tasks within their chosen assignee and status context without frustrating repetitive filter reselection.

---

## 5. Bug Hunt

Consider the provided helper function:
```javascript
function isTaskOverdue(task) {
  const today = new Date().toISOString().split('T')[0];
  return task.dueDate < today;
}
```

### Bug 1: Timezone Handling
`new Date().toISOString()` formats the timestamp in Universal Coordinated Time (UTC). In timezones ahead of or behind UTC (such as IST UTC+05:30 or Americas timezones), the UTC calendar date differs from the user's local calendar date for several hours each day, causing tasks to be prematurely or belatedly flagged as overdue.

### Bug 2: Missing or Invalid Data Handling
The function does not validate whether `task.dueDate` is present, valid, or null. When `dueDate` is `null`, `undefined`, or a malformed non-date string, JavaScript evaluates `null < today` via type coercion to `0 < today`, which evaluates to `false` without throwing an error, silently failing to report invalid data and producing incorrect results. Furthermore, the function fails to account for whether the task is already completed.

---

## 6. AI Usage

### Antigravity Usage
Google Antigravity was used as an AI pair programming environment to assist in scaffolding project architecture, generating boilerplate configurations, and formatting tests.

### What Was AI-Generated
- Initial build configurations (`build.gradle.kts`, `libs.versions.toml`, `settings.gradle.kts`).
- XML layout structural templates for Material 3 Cards, Toolbars, and ChipGroups.
- Initial boilerplate for Retrofit service interfaces and DTOs.

### What Was Manually Implemented or Verified
- **Overdue Date Calculation (`DateUtils.kt`)**: Implemented safe, timezone-aware date parsing using `java.time.LocalDate` with dependency-injected `Clock` to guarantee immunity to UTC skew and malformed inputs.
- **Architectural Separation**: Clean architecture domain layers (`Task`, `TaskFilter`, `TaskSummary`, Use Cases, `TaskRepository` interface) completely decoupled from Android UI and Retrofit DTOs.
- **Optimistic Update Rollback Logic**: Implemented immediate local state mutation in `TaskListViewModel` and `TaskDetailViewModel` paired with automatic rollback and user notification upon server error.
- **Filter Persistence Pattern**: Configured shared `activityViewModels()` scoping to ensure filter selection survives backstack navigation.
- **Backend Schema & Seeding**: Configured SQLite schema with checks, non-destructive startup seeding of the exact 15 assessment tasks, query parameter combinations, and field-level validation errors for `400 Bad Request`.
- **Adaptive Vector Drawables & Layout Polish**: Created custom vector drawables and verified visual hierarchy, typography, and color contrast.

### What Tests Were Executed
- **Backend Tests (`npm test` in `backend/`)**:
  - **16 passing tests** (0 failures) covering:
    - `GET /health` (200 OK)
    - `GET /api/tasks` (returns all 15 seeded tasks)
    - `GET /api/tasks?assignee=aarav` (assignee filter)
    - `GET /api/tasks?status=active` (status filter active)
    - `GET /api/tasks?status=completed` (status filter completed)
    - `GET /api/tasks?assignee=aarav&status=completed` (combined filter)
    - `GET /api/tasks?assignee=nonexistent&status=completed` (Edge Case 3: empty result)
    - `POST /api/tasks` (creates valid task, 201)
    - `POST /api/tasks` with null due date (Edge Case 1: creates task with null due date, 201)
    - `POST /api/tasks` rejecting empty or missing title (Edge Case 4: 400 Bad Request with field errors)
    - `POST /api/tasks` rejecting invalid priority (400 Bad Request)
    - `POST /api/tasks` rejecting invalid assignee (400 Bad Request)
    - `POST /api/tasks` rejecting invalid due date format (400 Bad Request)
    - `PATCH /api/tasks/:id/toggle` (toggles completed false -> true -> false)
    - `PATCH /api/tasks/:id/toggle` (404 for unknown task)
    - SQLite persistence across server restart (retains custom created tasks without re-seeding)
- **Android Unit Tests (`./gradlew testDebugUnitTest` in `android/`)**:
  - **25 passing tests** (0 failures, 100% success rate) covering:
    - `DateUtilsTest` (10 tests): Past overdue calculation, today due date, future due date, null due date handling (Edge Case 1), blank due date, completed overdue task immunity (Edge Case 2), malformed date handling, null parsing, and display date formatting.
    - `TaskFilterTest` (7 tests): Default filter, assignee filtering, active status filtering, completed status filtering, combined filter matching, empty combined filter results (Edge Case 3), and `TaskSummary` counters calculation.
    - `TaskListViewModelTest` (5 tests): State transitions (Loading -> Success, Loading -> Error), filter persistence across reloads (Decision 2), empty filtered results (Edge Case 3), and optimistic update with rollback on server failure (Decision 1).
    - `AddTaskViewModelTest` (3 tests): Client-side title validation, successful submission with navigation event, and server 400 validation error mapping onto field errors while retaining entered form data (Edge Case 4).

### Remaining Limitations
- Cleartext HTTP is utilized for local development networking (`http://10.0.2.2:3000`); a production deployment would require an HTTPS endpoint with TLS termination.
- Assignees are statically modeled to simulate multiple users (Aarav, Priya, Rohan) per assessment specifications without full user authentication.
