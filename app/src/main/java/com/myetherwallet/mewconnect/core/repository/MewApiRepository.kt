package com.myetherwallet.mewconnect.core.repository

import com.myetherwallet.mewconnect.content.api.mew.MewApiService
import com.myetherwallet.mewconnect.content.data.AnalyticsEvent
import com.myetherwallet.mewconnect.content.data.AnalyticsEventsRequest
import com.myetherwallet.mewconnect.core.platform.Either
import com.myetherwallet.mewconnect.core.platform.Failure
import com.myetherwallet.mewconnect.core.platform.NetworkHandler
import retrofit2.Call
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Created by BArtWell on 26.03.2020.
 */

interface MewApiRepository {

    fun submit(iso: String, events: List<AnalyticsEvent>): Either<Failure, Any>

    class Network
    @Inject constructor(private val networkHandler: NetworkHandler,
                        private val service: MewApiService) : MewApiRepository {

        override fun submit(iso: String, events: List<AnalyticsEvent>): Either<Failure, Any> {
            return when (networkHandler.isConnected) {
                true -> request(service.submit("android", iso, AnalyticsEventsRequest(events))) { it }
                false, null -> Either.Left(Failure.NetworkConnection())
            }
        }

        private fun <T, R> request(call: Call<T>, transform: (T) -> R): Either<Failure, R> {
            return try {
                val response = call.execute()
                when (response.isSuccessful) {
                    true -> {
                        val body = response.body()
                        if (body == null) {
                            Either.Left(Failure.ServerError(IllegalStateException("Body is empty")))
                        } else {
                            Either.Right(transform(body))
                        }
                    }
                    false -> Either.Left(Failure.ServerError(HttpException(response)))
                }
            } catch (exception: Throwable) {
                exception.printStackTrace()
                Either.Left(Failure.UnknownError(exception))
            }
        }
    }
}
