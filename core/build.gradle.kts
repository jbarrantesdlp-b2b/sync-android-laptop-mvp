plugins {
    kotlin("jvm") version "1.8.22"
    id("java-library")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("androidx.datastore:datastore-preferences:1.1.0")
}
