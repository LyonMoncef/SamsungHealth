import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Module d'analyse de Nightfall : Kotlin pur, aucune dépendance Android.
// Contient le contrat de données (spec 2026-09-23-phase1-data-foundation, DT-1/DT-2)
// et, plus tard, les algorithmes (périodogramme, τ, NPCRA).
plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
