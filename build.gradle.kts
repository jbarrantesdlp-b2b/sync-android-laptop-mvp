plugins {
    kotlin("jvm") version "1.8.22" apply false
    id("com.android.application") version "8.1.0" apply false
    id("com.android.library") version "8.1.0" apply false
}
ext["compose_version"] = "1.4.7"
ext["glance_version"] = "1.0.0"
ext["coroutines_version"] = "1.7.3"
ext["datastore_version"] = "1.1.0"
ext["okhttp_version"] = "4.11.0"
ext["work_version"] = "2.8.1"
