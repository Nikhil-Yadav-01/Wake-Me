package com.nikhil.wakeme.util

/**
 * A generic sealed class that represents the state of a resource.
 * It can be in one of four states: Loading, Success, Error, or Empty.
 */
sealed class Resource<T> {
    /**
     * Represents a successful state with data.
     * @param data The data of type T.
     */
    data class Success<T>(val data: T) : Resource<T>()

    /**
     * Represents an error state with a message.
     * @param message A string describing the error.
     */
    data class Error<T>(val message: String) : Resource<T>()

    /**
     * Represents the loading state.
     */
    object Loading : Resource<Nothing>()

    /**
     * Represents a state where the resource is empty (e.g., an empty list).
     */
    object Empty : Resource<Nothing>()
}
