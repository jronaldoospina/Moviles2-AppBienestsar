import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 37

    // ============ FIRMA DEL RELEASE ============
    // Las credenciales NO viven en este archivo (nunca se suben a git).
    // Se leen de keystore.properties (storeFile/storePassword/keyAlias/keyPassword)
    // o de las variables de entorno BIENESTAR_*.
    val propsFirma = Properties().apply {
        val archivo = project.rootProject.file("keystore.properties")
        if (archivo.exists()) {
            archivo.inputStream().use { stream -> load(stream) }
        }
    }

    fun credencial(prop: String, env: String): String? {
        val deEntorno = System.getenv(env)
        if (!deEntorno.isNullOrBlank()) return deEntorno
        val deArchivo = propsFirma.getProperty(prop)
        return if (deArchivo.isNullOrBlank()) null else deArchivo
    }

    val storeFilePath = credencial("storeFile", "BIENESTAR_STORE_FILE")
    val passStore = credencial("storePassword", "BIENESTAR_STORE_PASSWORD")
    val alias = credencial("keyAlias", "BIENESTAR_KEY_ALIAS")
    val passKey = credencial("keyPassword", "BIENESTAR_KEY_PASSWORD")

    val tieneFirma = storeFilePath != null && passStore != null
        && alias != null && passKey != null

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (tieneFirma) {
            create("release") {
                storeFile = rootProject.file(storeFilePath!!)
                storePassword = passStore
                keyAlias = alias
                keyPassword = passKey
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (tieneFirma) {
                signingConfig = signingConfigs.getByName("release")
                logger.lifecycle("Release: se firmara con el alias '$alias'")
            } else {
                logger.warn(
                    "Release SIN FIRMA: falta keystore.properties (storeFile, " +
                        "storePassword, keyAlias, keyPassword) o las variables BIENESTAR_*"
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // ============ ANDROIDX BASE ============
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.splashscreen)

    // ============ MATERIAL DESIGN 3 ============
    implementation(libs.material)

    // ============ NAVIGATION COMPONENT ============
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // ============ RECYCLERVIEW Y CARDVIEW ============
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.gridlayout)

    // ============ LIFECYCLE Y VIEWMODEL ============
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ============ FIREBASE ============
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)

    // ============ UI COMPONENTS ============
    implementation(libs.circleimageview)
    implementation(libs.glide)

    // ============ GRÁFICOS ============
    implementation(libs.mpandroidchart)

    // ============ PDF (Semáforo del Estudiante) ============
    implementation(libs.pdfbox.android)

    // ============ MAPA (OpenStreetMap / osmdroid - Ruta bus UPC Aguachica) ============
    implementation(libs.osmdroid.android)
    implementation(libs.play.services.location)

    // ============ ROOM (Persistencia Local) ============
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)

    // ============ TESTS ============
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    // 3.7.0+ usa getSystemService en vez de InputManager.getInstance por
    // reflexion, que fue removido en Android 16/17 (API 36/37).
    androidTestImplementation(libs.androidx.espresso.core)
}