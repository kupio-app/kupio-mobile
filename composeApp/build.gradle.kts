import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

fun runtimeConfigValue(
    name: String,
    debugDefault: String = "",
): String {
    return providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: localProperties.getProperty(name)
        ?: debugDefault
}

fun String.asBuildConfigString(): String {
    return "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

val releaseRuntimeConfigNames = listOf(
    "KUPIO_BACKEND_BASE_URL",
    "KUPIO_GOOGLE_SERVER_CLIENT_ID",
)

gradle.taskGraph.whenReady {
    val validatesReleaseRuntimeConfig = allTasks.any { task ->
        task.path == ":composeApp:assembleRelease" ||
            task.path == ":composeApp:bundleRelease" ||
            task.path == ":composeApp:packageRelease"
    }
    if (validatesReleaseRuntimeConfig) {
        val missing = releaseRuntimeConfigNames.filter { runtimeConfigValue(it).isBlank() }
        check(missing.isEmpty()) {
            "Missing release runtime config: ${missing.joinToString()}. " +
                "Set them via Gradle properties, environment variables, or local.properties."
        }
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.android)
        }
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kvault)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.voyager.navigator)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

android {
    namespace = "kupio.mobile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "kupio.mobile"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            buildConfigField(
                "String",
                "KUPIO_BACKEND_BASE_URL",
                runtimeConfigValue(
                    name = "KUPIO_BACKEND_BASE_URL",
                    debugDefault = "http://10.0.2.2:9988",
                ).asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "KUPIO_GOOGLE_SERVER_CLIENT_ID",
                runtimeConfigValue(
                    name = "KUPIO_GOOGLE_SERVER_CLIENT_ID",
                    debugDefault = "",
                ).asBuildConfigString(),
            )
        }
        getByName("release") {
            isMinifyEnabled = false
            buildConfigField(
                "String",
                "KUPIO_BACKEND_BASE_URL",
                runtimeConfigValue("KUPIO_BACKEND_BASE_URL").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "KUPIO_GOOGLE_SERVER_CLIENT_ID",
                runtimeConfigValue("KUPIO_GOOGLE_SERVER_CLIENT_ID").asBuildConfigString(),
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}
