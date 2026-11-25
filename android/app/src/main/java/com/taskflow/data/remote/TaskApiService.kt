package com.taskflow.data.remote

import com.taskflow.data.model.CreateTaskRequest
import com.taskflow.data.model.TaskDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TaskApiService {

    @GET("api/tasks")
    suspend fun getTasks(
        @Query("assignee") assignee: String? = null,
        @Query("status") status: String? = null
    ): Response<List<TaskDto>>

    @GET("api/tasks/{id}")
    suspend fun getTaskById(
        @Path("id") id: String
    ): Response<TaskDto>

    @POST("api/tasks")
    suspend fun createTask(
        @Body request: CreateTaskRequest
    ): Response<TaskDto>

    @PATCH("api/tasks/{id}/toggle")
    suspend fun toggleTask(
        @Path("id") id: String
    ): Response<TaskDto>
}
