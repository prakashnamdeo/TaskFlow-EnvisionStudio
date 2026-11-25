package com.taskflow.data.repository

import com.google.gson.Gson
import com.taskflow.data.model.ApiErrorResponse
import com.taskflow.data.model.CreateTaskRequest
import com.taskflow.data.remote.TaskApiService
import com.taskflow.domain.model.Assignee
import com.taskflow.domain.model.Priority
import com.taskflow.domain.model.Task
import com.taskflow.domain.model.TaskFilter
import com.taskflow.domain.repository.TaskRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

class ApiException(
    message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
    val statusCode: Int = 500
) : Exception(message)

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val apiService: TaskApiService,
    private val gson: Gson
) : TaskRepository {

    override suspend fun getTasks(filter: TaskFilter?): Result<List<Task>> {
        return try {
            val assigneeParam = filter?.assignee?.id
            val statusParam = filter?.status?.value
            val response = apiService.getTasks(assignee = assigneeParam, status = statusParam)
            handleResponse(response) { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    override suspend fun getTaskById(id: String): Result<Task> {
        return try {
            val response = apiService.getTaskById(id)
            handleResponse(response) { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    override suspend fun createTask(
        title: String,
        description: String?,
        dueDate: String?,
        priority: Priority,
        assignee: Assignee
    ): Result<Task> {
        return try {
            val request = CreateTaskRequest(
                title = title,
                description = description,
                dueDate = dueDate,
                priority = priority.value,
                assigneeId = assignee.id
            )
            val response = apiService.createTask(request)
            handleResponse(response) { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    override suspend fun toggleTask(id: String): Result<Task> {
        return try {
            val response = apiService.toggleTask(id)
            handleResponse(response) { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    private fun <T, R> handleResponse(response: Response<T>, transform: (T) -> R): Result<R> {
        if (response.isSuccessful) {
            val body = response.body()
            return if (body != null) {
                Result.success(transform(body))
            } else {
                Result.failure(ApiException("Empty server response", statusCode = response.code()))
            }
        }

        // Parse error body
        val errorBodyStr = response.errorBody()?.string()
        val errorResponse = try {
            if (!errorBodyStr.isNullOrBlank()) {
                gson.fromJson(errorBodyStr, ApiErrorResponse::class.java)
            } else null
        } catch (e: Exception) {
            null
        }

        val message = errorResponse?.error ?: "Server error: HTTP ${response.code()}"
        val fieldErrors = errorResponse?.errors ?: emptyMap()
        return Result.failure(ApiException(message, fieldErrors, response.code()))
    }

    private fun mapException(e: Exception): Exception {
        return when (e) {
            is IOException -> ApiException("Network error. Please check your internet connection and verify backend is running.", statusCode = -1)
            is ApiException -> e
            else -> ApiException(e.message ?: "An unexpected error occurred")
        }
    }
}
