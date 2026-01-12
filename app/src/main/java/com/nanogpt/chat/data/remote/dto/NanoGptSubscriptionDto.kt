package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NanoGptSubscriptionDto(
    val active: Boolean,
    val allowOverage: Boolean,
    val enforceDailyLimit: Boolean,
    val graceUntil: String?,
    val daily: UsagePeriodDto,
    val monthly: UsagePeriodDto,
    val limits: LimitsDto,
    val period: PeriodDto,
    val state: String
)

@Serializable
data class UsagePeriodDto(
    val used: Int,
    val remaining: Int,
    val percentUsed: Double,
    val resetAt: Long
)

@Serializable
data class LimitsDto(
    val daily: Int,
    val monthly: Int
)

@Serializable
data class PeriodDto(
    val currentPeriodEnd: String
)
