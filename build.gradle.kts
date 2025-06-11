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
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(findProperty("sonatype.username") as String? ?: System.getenv("OSSRH_USERNAME"))
            password.set(findProperty("sonatype.password") as String? ?: System.getenv("OSSRH_PASSWORD"))
            stagingProfileId.set(findProperty("sonatype.stagingProfileId") as String? ?: System.getenv("OSSRH_STAGING_PROFILE_ID"))
        }
    }
}

ext["versionCode"] = 1
ext["versionName"] = "1.0.0"
ext["compileSdk"] = 35
ext["targetSdk"] = 35
ext["minSdk"] = 24