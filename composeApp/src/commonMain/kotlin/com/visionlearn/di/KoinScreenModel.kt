package com.visionlearn.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import org.koin.compose.LocalKoinScope
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope

/**
 * Custom koinScreenModel implementation to replace voyager-koin
 * which has compatibility issues with Kotlin 2.1 on iOS
 */
@Composable
inline fun <reified T : ScreenModel> Screen.koinScreenModel(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T {
    val koinScope = LocalKoinScope.current
    return rememberScreenModel {
        koinScope.get<T>(qualifier, parameters)
    }
}
