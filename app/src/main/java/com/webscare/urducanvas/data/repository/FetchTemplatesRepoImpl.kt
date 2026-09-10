package com.webscare.urducanvas.data.repository

import android.util.Log
import com.webscare.urducanvas.common.sealed.Response
import com.webscare.urducanvas.data.model.TemplatesResponse
import com.webscare.urducanvas.data.remote.EndPointsInterface
import com.webscare.urducanvas.domain.repo.FetchTemplatesRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject
/**
 * Log tag for this file. Was android.content.ContentValues.TAG, an accidental import that
 * filed every one of these messages under "ContentValues".
 */
private const val TAG = "FetchTemplatesRepoImpl"

class FetchTemplatesRepoImpl @Inject constructor(
    private val api: EndPointsInterface
) : FetchTemplatesRepo {

    override fun fetchTemplates(): Flow<Response<TemplatesResponse>> = channelFlow {
        try {
            trySend(Response.Loading)
            val response = api.getAllTemplates()

            Log.e(TAG, "fetchTemplates: $response")
            trySend(Response.Success(response))
        } catch (e: Exception) {
            Log.e(TAG, "fetchTemplates: $e")
            if (e.message?.contains("Connection reset") == true){
                trySend(Response.Error("Unstable Internet Connection!"))
            }else{
                trySend(Response.Error("Unexpected Error Occurred ${e.message}"))
            }
        }
    }
}
