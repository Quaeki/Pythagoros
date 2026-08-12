package com.example.pythagoros.backend

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val ok: Boolean,
    val service: String,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
)

@Serializable
data class AuthRequestCodeRequest(
    val phone: String,
)

@Serializable
data class AuthRequestCodeResponse(
    val requestId: String,
    val expiresInSeconds: Long,
    val debugCode: String? = null,
)

@Serializable
data class AuthVerifyCodeRequest(
    val requestId: String,
    val code: String,
)

@Serializable
data class AuthVerifyCodeResponse(
    val userId: String,
    val phone: String,
    val sessionToken: String,
    val isNewUser: Boolean,
)

@Serializable
data class AuthProviderSignInRequest(
    val provider: String,
    val providerUserId: String? = null,
    val idToken: String? = null,
    val accessToken: String? = null,
    val email: String? = null,
    val displayName: String? = null,
)

@Serializable
data class AuthSessionResponse(
    val userId: String,
    val phone: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val sessionToken: String,
    val isNewUser: Boolean,
)

@Serializable
data class PremiumSolveRequest(
    val expression: String,
    val problemType: String? = null,
    val localSteps: List<SolutionStepDto> = emptyList(),
    val locale: String = "ru",
    val requireGraph: Boolean = true,
)

@Serializable
data class PremiumSolveResponse(
    val expression: String,
    val problemType: String? = null,
    val answer: String,
    val steps: List<SolutionStepDto>,
    val graph: GraphDto? = null,
    val model: String,
    val source: String = "gemini",
)

@Serializable
data class SolutionStepDto(
    val title: String,
    val formula: String,
    val explanation: String,
)

@Serializable
data class GraphDto(
    val title: String? = null,
    val variable: String? = "x",
    val expression: String? = "",
    val notes: List<String> = emptyList(),
    val kind: String? = null,
    val viewport: ViewportDto? = null,
    val primitives: List<VisualPrimitiveDto> = emptyList(),
)

@Serializable
internal data class GeminiSolvePayload(
    val answer: String,
    val steps: List<SolutionStepDto>,
    val graph: GraphDto? = null,
)

@Serializable
data class ViewportDto(
    val xMin: Double? = null,
    val xMax: Double? = null,
    val yMin: Double? = null,
    val yMax: Double? = null,
)

@Serializable
data class PointDto(
    val x: Double,
    val y: Double,
)

@Serializable
data class VisualPrimitiveDto(
    val type: String,
    val id: String? = null,
    val role: String? = null,
    val points: List<PointDto> = emptyList(),
    val from: PointDto? = null,
    val to: PointDto? = null,
    val center: PointDto? = null,
    val radius: Double? = null,
    val startAngle: Double? = null,
    val endAngle: Double? = null,
    val at: PointDto? = null,
    val text: String? = null,
    val label: String? = null,
    val dashed: Boolean = false,
    val filled: Boolean = false,
    val closed: Boolean? = null,
)
