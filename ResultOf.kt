

sealed interface Error
data class HttpError(val code: Int, val message: String) : Error
data class GenericError(val throwable: Throwable) : Error

sealed interface ResultOf<T>
data class IsSuccess<T>(val data: T) : ResultOf<T>
data class IsFailure<T>(val error: Error) : ResultOf<T>

inline fun <T> getResultOf(action: ()->T): ResultOf<T>{
    return try{
        IsSuccess(action())
    }catch (e: Throwable){
        IsFailure(GenericError(e))
    }
}

inline fun <T> ResultOf<T>.onSuccess(action: (T) -> Unit): ResultOf<T> {
    if (this is IsSuccess) {
        action(data)
    }
    return this
}

inline fun <T> ResultOf<T>.onFailure(action: (Error) -> Unit): ResultOf<T> {
    if (this is IsFailure) action(error)
    return this
}

inline fun <T> ResultOf<T>.onHttpError(action: (code: Int, message: String) -> Unit): ResultOf<T> {
    if (this is IsFailure && this.error is HttpError) action(error.code, error.message)
    return this
}

inline fun <T> ResultOf<T>.onGenericError(action: (Throwable) -> Unit): ResultOf<T> {
    if (this is IsFailure && this.error is GenericError) action(error.throwable)
    return this
}

inline fun <T> ResultOf<T>.onEither(action: (T?, Error?) -> Unit): ResultOf<T> {
    if (this is IsSuccess) {
        action(data, null)
    } else if (this is IsFailure) {
        action(null, error)
    }
    return this
}
