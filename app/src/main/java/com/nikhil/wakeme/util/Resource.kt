package com.nikhil.wakeme.util

// Class to check status
open class Resource<T> {
    class Initial<T> : Resource<T>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String) : Resource<T>()
    class Loading<T> : Resource<T>()
    class Empty<T> : Resource<T>()
}
