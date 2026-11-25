# TaskFlow — Android Application

Native Android application built with Kotlin, MVVM architecture, Clean Architecture principles, Jetpack components, Material 3, and Hilt dependency injection.

## Architecture & Tech Stack
- **Language**: Kotlin 2.0.21
- **UI Framework**: Native Android XML Layouts with Material 3 components & ViewBinding
- **Architecture**: Clean Architecture + MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt (Dagger) with KSP
- **Asynchronous & State**: Kotlin Coroutines & StateFlow (Lifecycle-aware repeatOnLifecycle)
- **Networking**: Retrofit 2.11.0 + OkHttp 4.12.0 + Gson
- **Navigation**: Jetpack Navigation Component
- **Unit Testing**: JUnit 4, MockK, Kotlinx Coroutines Test, Turbine, AndroidX Arch Core Testing

## Package Structure
```
com.taskflow
├── data
│   ├── model       (TaskDto, CreateTaskRequest, ApiErrorResponse)
│   ├── remote      (TaskApiService)
│   └── repository  (TaskRepositoryImpl, ApiException)
├── domain
│   ├── model       (Task, Priority, Assignee, TaskFilter, TaskSummary)
│   ├── repository  (TaskRepository interface)
│   └── usecase     (GetTasksUseCase, ToggleTaskUseCase, CreateTaskUseCase)
├── presentation
│   ├── common      (UiState)
│   ├── tasklist    (TaskListFragment, TaskListViewModel, TaskAdapter)
│   ├── taskdetail  (TaskDetailFragment, TaskDetailViewModel)
│   └── addtask     (AddTaskFragment, AddTaskViewModel)
├── di              (NetworkModule, RepositoryModule)
└── utils           (DateUtils, safe timezone and overdue calculation)
```

## Running Unit Tests
```bash
./gradlew testDebugUnitTest
```

## Running the App on a Physical Device (Same Wi-Fi)
1. **Ensure Both Devices are on the Same Wi-Fi**:
   - Development PC IP: `192.168.0.110` (or configured via `server.ip` in `local.properties`).
2. **Start the Backend**:
   ```bash
   cd backend
   npm start
   ```
3. **Build and Install the App**:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```
   The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
   - Install via USB once: `./gradlew installDebug`, OR
   - Directly transfer and install `app-debug.apk` onto your phone.
4. **Network Auto-Resolution**:
   - The app connects directly to `http://192.168.0.110:3000/` across your Wi-Fi network.
   - If connected via USB cable with `adb reverse tcp:3000 tcp:3000`, the app automatically falls back to `http://127.0.0.1:3000/` if Wi-Fi drops.
   - If running inside an emulator, it automatically switches to `http://10.0.2.2:3000/`.
