package com.example.snaptag.utils

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

object FeedbackManager {
    suspend fun success(snackbar: SnackbarHostState, message: String) {
        snackbar.showSnackbar(message)
    }

    suspend fun error(snackbar: SnackbarHostState, message: String): Boolean {
        val result = snackbar.showSnackbar(
            message = message,
            actionLabel = "Retry"
        )
        return result == SnackbarResult.ActionPerformed
    }
}
