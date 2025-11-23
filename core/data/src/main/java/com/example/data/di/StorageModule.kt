package com.example.data.di

import android.content.Context
import com.example.data.BuildConfig
import com.example.data.storage.YandexStorageSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import java.net.URI
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    const val REGION = BuildConfig.REGION
    const val ENDPOINT = BuildConfig.ENDPOINT
    const val BUCKET_NAME = BuildConfig.BUCKET_NAME

    const val ACCESS_KEY = BuildConfig.ACCESS_KEY
    const val SECRET_KEY = BuildConfig.SECRET_KEY

    @Provides
    @Singleton
    fun provideS3AsyncClient(): S3AsyncClient {
        val credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)
        )

        return S3AsyncClient.builder()
            .region(Region.of(REGION))
            .endpointOverride(URI.create(ENDPOINT))
            .credentialsProvider(credentialsProvider)
            .forcePathStyle(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideYandexStorageSource(
        @ApplicationContext context: Context,
        s3AsyncClient: S3AsyncClient
    ): YandexStorageSource {
        return YandexStorageSource(context, s3AsyncClient, BUCKET_NAME)
    }

}