package com.kaoyan.studyassistant.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences =
        AppPreferences(context)

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()
}
