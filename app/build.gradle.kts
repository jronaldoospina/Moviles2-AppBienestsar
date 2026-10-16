import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

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
        targetSdk = 35
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

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    // ============ ANDROIDX BASE ============
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ============ MATERIAL DESIGN 3 ============
    implementation("com.google.android.material:material:1.11.0")

    // ============ NAVIGATION COMPONENT ============
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // ============ RECYCLERVIEW Y CARDVIEW ============
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")

    // ============ LIFECYCLE Y VIEWMODEL ============
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // ============ FIREBASE ============
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // ============ UI COMPONENTS ============
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // ============ GRÁFICOS ============
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ============ PDF (Semáforo del Estudiante) ============
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // ============ MAPA (OpenStreetMap / osmdroid - Ruta bus UPC Aguachica) ============
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // ============ ROOM (Persistencia Local) ============
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    // ============ TESTS ============
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    // 3.7.0+ usa getSystemService en vez de InputManager.getInstance por
    // reflexion, que fue removido en Android 16/17 (API 36/37).
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}