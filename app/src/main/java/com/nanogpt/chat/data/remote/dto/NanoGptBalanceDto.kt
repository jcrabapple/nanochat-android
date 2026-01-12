package com.nanogpt.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NanoGptBalanceDto(
    val usd_balance: String,
    val nano_balance: String,
    val nanoDepositAddress: String
)
