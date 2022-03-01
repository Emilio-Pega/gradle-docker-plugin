plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("org.asciidoctor:asciidoctor-gradle-plugin:1.5.9.1")
    runtimeOnly("com.bmuschko:asciidoctorj-tabbed-code-extension:0.2")
    implementation("org.ajoberstar:grgit:1.9.1") {
        setForce(true)
    }
    implementation("org.ajoberstar:gradle-git:1.7.1")
    implementation("org.ajoberstar:gradle-git-publish:0.3.3")
    implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5")
    implementation("com.github.jengelman.gradle.plugins:shadow:5.0.0")
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

gradlePlugin {
    plugins {
        register("test-setup-plugin") {
            id = "com.bmuschko.gradle.docker.test-setup"
            implementationClass = "com.bmuschko.gradle.docker.TestSetupPlugin"
        }
        register("integration-test-plugin") {
            id = "com.bmuschko.gradle.docker.integration-test"
            implementationClass = "com.bmuschko.gradle.docker.IntegrationTestPlugin"
        }
        register("functional-test-plugin") {
            id = "com.bmuschko.gradle.docker.functional-test"
            implementationClass = "com.bmuschko.gradle.docker.FunctionalTestPlugin"
        }
        register("doc-test-plugin") {
            id = "com.bmuschko.gradle.docker.doc-test"
            implementationClass = "com.bmuschko.gradle.docker.DocumentationTestPlugin"
        }
        register("additional-artifacts-plugin") {
            id = "com.bmuschko.gradle.docker.additional-artifacts"
            implementationClass = "com.bmuschko.gradle.docker.AdditionalArtifactsPlugin"
        }
        register("shaded-artifacts-plugin") {
            id = "com.bmuschko.gradle.docker.shaded-artifacts"
            implementationClass = "com.bmuschko.gradle.docker.ShadedArtifactsPlugin"
        }
        register("user-guide-plugin") {
            id = "com.bmuschko.gradle.docker.user-guide"
            implementationClass = "com.bmuschko.gradle.docker.UserGuidePlugin"
        }
        register("documentation-plugin") {
            id = "com.bmuschko.gradle.docker.documentation"
            implementationClass = "com.bmuschko.gradle.docker.DocumentationPlugin"
        }
        register("release-plugin") {
            id = "com.bmuschko.gradle.docker.release"
            implementationClass = "com.bmuschko.gradle.docker.ReleasePlugin"
        }
    }
}