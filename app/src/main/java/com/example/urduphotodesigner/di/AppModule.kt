package com.example.urduphotodesigner.di

import android.content.Context
import com.example.urduphotodesigner.common.Constants
import com.example.urduphotodesigner.common.SocketFactoryWithTcpNoDelay
import com.example.urduphotodesigner.common.datastore.PreferencesDataStoreHelper
import com.example.urduphotodesigner.data.local.AppDatabase
import com.example.urduphotodesigner.data.remote.EndPointsInterface
import com.example.urduphotodesigner.data.repository.FetchFontsRepoImpl
import com.example.urduphotodesigner.data.repository.FetchImagesRepoImpl
import com.example.urduphotodesigner.data.repository.FontsRepoImpl
import com.example.urduphotodesigner.data.repository.ImagesRepoImpl
import com.example.urduphotodesigner.domain.repo.FetchFontsRepo
import com.example.urduphotodesigner.domain.repo.FetchImagesRepo
import com.example.urduphotodesigner.domain.repo.FontsRepo
import com.example.urduphotodesigner.domain.repo.ImagesRepo
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun providesWebApiInterface(): EndPointsInterface {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val gson = GsonBuilder()
            .setLenient()
            .create()
        val httpClient: OkHttpClient.Builder = OkHttpClient.Builder()
            .socketFactory(SocketFactoryWithTcpNoDelay())
            .addInterceptor(logging)
            .addInterceptor(Interceptor { chain ->
                val original: Request = chain.request()
                val originalHttpUrl: HttpUrl = original.url
                val url = originalHttpUrl.newBuilder()
                    .build()
                val requestBuilder: Request.Builder = original.newBuilder()
                    .url(url)
                val request: Request = requestBuilder.build()
                chain.proceed(request)
            })
            .readTimeout(120, TimeUnit.SECONDS)
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
        return Retrofit.Builder().baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient.build())
            .build().create(EndPointsInterface::class.java)
    }

    @Provides
    fun providesAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase(context)
    }

    @Provides
    @Singleton
    fun provideDataStoreHelper(@ApplicationContext context: Context): PreferencesDataStoreHelper {
        return PreferencesDataStoreHelper(context)
    }

    @Provides
    @Singleton
    fun provideFetchFontsRepo(api: EndPointsInterface): FetchFontsRepo {
        return FetchFontsRepoImpl(api)
    }

    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideFontsRepo(appDatabase: AppDatabase): FontsRepo {
        return FontsRepoImpl(appDatabase)
    }

    @Provides
    @Singleton
    fun provideFetchImagesRepo(api: EndPointsInterface): FetchImagesRepo {
        return FetchImagesRepoImpl(api)
    }

    @Provides
    @Singleton
    fun provideImagesRepo(appDatabase: AppDatabase): ImagesRepo {
        return ImagesRepoImpl(appDatabase)
    }

}