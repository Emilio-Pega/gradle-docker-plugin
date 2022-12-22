import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.text.SimpleDateFormat
import java.util.Date

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    groovy
    `java-gradle-plugin`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.jfrog.artifactory") version "4.30.1"
}

group = "com.bmuschko"
version = "9.0.1-pega"


val shaded by configurations.creating
val implementation = configurations.getByName("implementation")
implementation.extendsFrom(shaded)

val packagesToRelocate = listOf(
    "javassist",
    "org.glassfish",
    "org.jvnet",
    "jersey.repackaged",
    "com.fasterxml",
    "io.netty",
    "org.bouncycastle",
    "org.apache",
    "org.aopalliance",
    "org.scijava",
    "com.google",
    "javax.annotation",
    "javax.ws",
    "net.sf",
    "org.objectweb",
    "javax.activation"
)

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set(null as String?)
    configurations = listOf(shaded)
    mergeServiceFiles()
    for (pkg in packagesToRelocate) {
        relocate(pkg, "com.bmuschko.gradle.docker.shaded.$pkg")
    }
}

val jar: Jar by tasks
jar.enabled = false
tasks["assemble"].dependsOn(tasks.shadowJar)

repositories {
    mavenCentral()
}

dependencies {
    shaded(libs.bundles.docker.java)
    shaded(libs.activation)
    shaded(libs.asm)
    testImplementation(libs.spock.core) {
        exclude(group = "org.codehaus.groovy")
    }
    testImplementation(libs.zt.zip)
    //functionalTestImplementation(libs.commons.vfs2)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<GroovyCompile> {
    options.compilerArgs.add("-Xlint:-options")
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    groovyOptions.optimizationOptions?.put("all", false)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Implementation-Title"] = "Gradle Docker plugin"
        attributes["Implementation-Version"] = project.version
        attributes["Built-By"] = System.getProperty("user.name")
        attributes["Built-Date"] = SimpleDateFormat("MM/dd/yyyy").format(Date())
        attributes["Built-JDK"] = System.getProperty("java.version")
        attributes["Built-Gradle"] = gradle.gradleVersion
    }
}

gradlePlugin {
    plugins {
        create("docker-remote-api") {
            id = "com.bmuschko.docker-remote-api"
            displayName = "Gradle Docker Remote API Plugin"
            description = "Plugin that provides tasks for interacting with Docker via its remote API."
            implementationClass = "com.bmuschko.gradle.docker.DockerRemoteApiPlugin"
        }
        create("docker-java-application") {
            id = "com.bmuschko.docker-java-application"
            displayName = "Gradle Docker Java Application Plugin"
            description = "Plugin that provides conventions for building and publishing Docker images for Java applications."
            implementationClass = "com.bmuschko.gradle.docker.DockerJavaApplicationPlugin"
        }
        create("docker-spring-boot-application") {
            id = "com.bmuschko.docker-spring-boot-application"
            displayName = "Gradle Docker Spring Boot Application Plugin"
            description = "Plugin that provides conventions for building and publishing Docker images for Spring Boot applications."
            implementationClass = "com.bmuschko.gradle.docker.DockerSpringBootApplicationPlugin"
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("pluginMaven") {
                setArtifacts(listOf(project.tasks.shadowJar))
            }
        }
    }
    publishing.publications.map { it.name }.forEach {
        val pubName = it
        project.tasks.artifactoryPublish {
            publications(pubName)
        }
    }
}
apply(from = "artifactory.gradle")