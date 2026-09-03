plugins {
    kotlin("jvm") version "1.8.22"
    id("java-library")
    kotlin("kapt")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("org.json:json:20231013")
    implementation("androidx.room:room-ktx:2.5.2")
    implementation(project(":core"))
    implementation(project(":data"))
    
    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.mockito:mockito-core:5.3.1")
}
