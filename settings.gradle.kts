pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "anchor-android"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":core:common")
include(":core:model")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:ui")
include(":core:testing")
include(":core:screenshot-testing")
include(":core:engine")
include(":core:nfc")
include(":core:qr")
include(":core:triggers")

include(":service:blocker")
include(":service:notifications")

include(":feature:onboarding")
include(":feature:home")
include(":feature:modes")
include(":feature:anchors")
include(":feature:triggers")
include(":feature:insights")
include(":feature:settings")

include(":widget")

include(":baselineprofile")
include(":benchmark")

include(":lint-rules")
