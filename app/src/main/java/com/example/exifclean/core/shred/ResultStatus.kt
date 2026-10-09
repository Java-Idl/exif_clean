package com.example.exifclean.core.shred

sealed class ResultStatus {
    object LOSSLESS_CLEANED : ResultStatus()
    object REENCODED_CLEANED : ResultStatus()
    object CRYPTO_SHREDDED : ResultStatus()
    object OVERWRITTEN_BEST_EFFORT : ResultStatus()
    object UNLINKED_ONLY : ResultStatus()
    data class FAILED(val reason: String) : ResultStatus()

    val name: String
        get() = when (this) {
            LOSSLESS_CLEANED -> "LOSSLESS_CLEANED"
            REENCODED_CLEANED -> "REENCODED_CLEANED"
            CRYPTO_SHREDDED -> "CRYPTO_SHREDDED"
            OVERWRITTEN_BEST_EFFORT -> "OVERWRITTEN_BEST_EFFORT"
            UNLINKED_ONLY -> "UNLINKED_ONLY"
            is FAILED -> "FAILED($reason)"
        }
}
