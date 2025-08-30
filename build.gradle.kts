// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.nexus.sonatype)
    alias(libs.plugins.bom.generator).apply(false)
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(findProperty("sonatypeUsername") as String? ?: System.getenv("OSSRH_USERNAME"))
            password.set(findProperty("sonatypePassword") as String? ?: System.getenv("OSSRH_PASSWORD"))
            stagingProfileId.set(findProperty("signing.keyId") as String? ?: System.getenv("OSSRH_STAGING_PROFILE_ID"))
        }
    }
}

ext["versionCode"] = 9
ext["versionName"] = "1.0.10"
ext["compileSdk"] = 35
ext["targetSdk"] = 35
ext["minSdk"] = 24