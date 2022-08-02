package com.bmuschko.gradle.docker.tasks.image

import com.bmuschko.gradle.docker.tasks.AbstractDockerRemoteApiTask
import com.bmuschko.gradle.docker.internal.IOUtils
import com.github.dockerjava.api.command.SaveImagesCmd
import com.github.dockerjava.api.command.SaveImagesCmd.TaggedImage
import com.github.dockerjava.core.command.SaveImagesCmdImpl
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

import java.util.zip.GZIPOutputStream

@CompileStatic
class DockerSaveImage extends AbstractDockerRemoteApiTask {

    /**
     * The images including repository, image name and tag to be saved e.g. {@code vieux/apache:2.0}.
     *
     * @since 8.0.0
     */
    @Input
    final SetProperty<String> images = project.objects.setProperty(String)

    @Input
    @Optional
    final Property<Boolean> useCompression = project.objects.property(Boolean)

    /**
     * Where to save image.
     */
    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    DockerSaveImage() {
        useCompression.set(false)
        onlyIf {
            images.getOrNull()
        }
    }

    // part of work-around for https://github.com/docker-java/docker-java/issues/1872
    @CompileDynamic
    private SaveImagesCmd.Exec getExecution() {
        dockerClient.saveImagesCmd().@execution
    }

    @Override
    void runRemoteCommand() {
        Set<String> images = images.getOrElse([] as Set)
        // part of work-around for https://github.com/docker-java/docker-java/issues/1872
        SaveImagesCmd saveImagesCmd = new SaveImagesCmdImpl(execution) {
            @Override
            List<TaggedImage> getImages() {
                images.collect {
                    { -> it } as TaggedImage
                }
            }
        }
        InputStream image = saveImagesCmd.exec()
        OutputStream os
        try {
            FileOutputStream fs = new FileOutputStream(destFile.get().asFile)
            os = fs
            if (useCompression.get()) {
                os = new GZIPOutputStream(fs)
            }
            try {
                IOUtils.copy(image, os)
            } catch (IOException e) {
                throw new GradleException("Can't save image.", e)
            } finally {
                IOUtils.closeQuietly(image)
            }
        }
        finally {
            IOUtils.closeQuietly(os)
        }
    }
}
