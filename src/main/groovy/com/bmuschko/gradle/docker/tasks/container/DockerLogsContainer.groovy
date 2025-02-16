/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmuschko.gradle.docker.tasks.container

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.LogContainerCmd
import com.github.dockerjava.api.model.Frame
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

/**
 * Copies the container logs into standard out/err, the same as the `docker logs` command. The container output
 * to standard out will go to standard out, and standard err to standard err.
 */
@CompileStatic
class DockerLogsContainer extends DockerExistingContainer {

    /**
     * Set to true to follow the output, which will cause this task to block until the container exists.
     * Default is unspecified (docker defaults to false).
     */
    @Input
    @Optional
    final Property<Boolean> follow = project.objects.property(Boolean)

    /**
     * Set to true to copy all output since the container has started. For long running containers or containers
     * with a lot of output this could take a long time. This cannot be set if #tailCount is also set. Setting to false
     * leaves the decision of how many lines to copy to docker.
     * Default is unspecified (docker defaults to true).
     */
    @Input
    @Optional
    final Property<Boolean> tailAll = project.objects.property(Boolean)

    /**
     * Limit the number of lines of existing output. This cannot be set if #tailAll is also set.
     * Default is unspecified (docker defaults to all lines).
     */
    @Input
    @Optional
    final Property<Integer> tailCount = project.objects.property(Integer)

    /**
     * Include standard out.
     * Default is true.
     */
    @Input
    @Optional
    final Property<Boolean> stdOut = project.objects.property(Boolean)

    /**
     * Include standard err.
     * Default is true.
     */
    @Input
    @Optional
    final Property<Boolean> stdErr = project.objects.property(Boolean)

    /**
     * Set to the true to include a timestamp for each line in the output.
     * Default is unspecified (docker defaults to false).
     */
    @Input
    @Optional
    final Property<Boolean> showTimestamps = project.objects.property(Boolean)

    /**
     * Limit the output to lines on or after the specified date.
     * Default is unspecified (docker defaults to all lines).
     */
    @Input
    @Optional
    final Property<Date> since = project.objects.property(Date)

    /**
     * Sink to write log output into.
     */
    @Input
    @Optional
    Writer sink

    def setSink(Writer sink) {
        if (sink != null) {
            notCompatibleWithConfigurationCache("Setting sink is not compatible with configuration cache")
            this.sink = sink
        } else {
            isCompatibleWithConfigurationCache()
            this.sink = null
        }
    }

    // Allows subclasses to carry their own logic
    @Internal
    protected Date getInternalSince() {
        return since.getOrNull()
    }

    DockerLogsContainer() {
        stdOut.convention(true)
        stdErr.convention(true)
    }

    @Override
    void runRemoteCommand() {
        logger.quiet "Logs for container with ID '${containerId.get()}'."
        logAndProcessResponse(dockerClient)
    }

    // method used for sub-classes who wish to invoke this task
    // multiple times but don't want the logging message to be
    // printed for every iteration.
    void logAndProcessResponse(DockerClient dockerClient) {
        LogContainerCmd logCommand = dockerClient.logContainerCmd(containerId.get())
        setContainerCommandConfig(logCommand)
        logCommand.exec(createCallback(nextHandler))?.awaitCompletion()
    }

    private ResultCallback.Adapter<Frame> createCallback(Action nextHandler) {
        if(sink && nextHandler) {
            throw new GradleException("Define either sink or onNext")
        }
        if(sink) {
            return new ResultCallback.Adapter<Frame>() {
                @Override
                void onNext(Frame frame) {
                    switch (frame.streamType as String) {
                        case 'STDOUT':
                        case 'RAW':
                        case 'STDERR':
                            sink.append(new String(frame.payload))
                            sink.flush()
                            break
                    }
                    super.onNext(frame)
                }
            }
        }
        if(nextHandler) {
            return new ResultCallback.Adapter<Frame>() {
                @Override
                void onNext(Frame frame) {
                    nextHandler.execute(frame)
                    super.onNext(frame)
                }
            }
        }

        new ResultCallback.Adapter<Frame>() {
            @Override
            void onNext(Frame frame) {
                switch (frame.streamType as String) {
                    case 'STDOUT':
                    case 'RAW':
                        logger.quiet(new String(frame.payload).replaceFirst(/\s+$/, ''))
                        break
                    case 'STDERR':
                        logger.error(new String(frame.payload).replaceFirst(/\s+$/, ''))
                        break
                }
                super.onNext(frame)
            }
        }
    }

    private void setContainerCommandConfig(LogContainerCmd logsCommand) {
        if (follow.getOrNull() != null) {
            logsCommand.withFollowStream(follow.get())
        }

        if (showTimestamps.getOrNull() != null) {
            logsCommand.withTimestamps(showTimestamps.get())
        }

        logsCommand.withStdOut(stdOut.get()).withStdErr(stdErr.get())

        if (tailAll.getOrNull() && tailCount.getOrNull()) {
            throw new InvalidUserDataException("Conflicting parameters: only one of tailAll and tailCount can be specified")
        }

        if (tailAll.getOrNull()) {
            logsCommand.withTailAll()
        } else if (tailCount.getOrNull() != null) {
            logsCommand.withTail(tailCount.get())
        }

        Date since = getInternalSince()
        if (since) {
            logsCommand.withSince((int) (since.time / 1000))
        }
    }
}

