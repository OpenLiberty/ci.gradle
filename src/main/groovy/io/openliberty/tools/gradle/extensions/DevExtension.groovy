/**
 * (C) Copyright IBM Corporation 2020, 2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openliberty.tools.gradle.extensions

class DevExtension {
    // If set to `true`, run the server in the container specified by the `containerfile` parameter. 
    // Setting this to `true` and using the `libertyDev` task is equivalent to using the `libertyDevc` task. 
    // The default value is `false` when the `libertyDev` task is used, and `true` when the `libertyDevc` task is used.
    boolean container = false

    // Location of a Containerfile or Dockerfile to be used by dev mode to build the image for the container that will run your Liberty server. 
    // The default location is `Containerfile` or `Dockerfile` in the project root. This attribute replaces the deprecated `dockerfile` attribute and will take precedence.
    File containerfile

    // The container build context directory to be used by dev mode for the container `build` command.  
    // The default location is the directory of the Containerfile/Dockerfile. This attribute replaces the deprecated `dockerBuildContext` attribute and will take precedence.
    File containerBuildContext

    // Specifies options to add to the container `run` command when using dev mode to launch your server in a container. 
    // For example, `-e key=value` is recognized by `docker run` to define an environment variable with the name `key` and value `value`. 
    // This attribute replaces the deprecated `dockerRunOpts` attribute and will take precedence.
    String containerRunOpts

    // Maximum time to wait (in seconds) for the completion of the container operation to build the image. 
    // The value must be an integer greater than 0. The default value is `600` seconds. 
    // This attribute replaces the deprecated `dockerBuildTimeout` attribute and will take precedence.
    int containerBuildTimeout

    // If set to `true`, dev mode will not publish the default container port mappings of `9080:9080` (HTTP) and `9443:9443` (HTTPS). 
    // Use this option if you would like to specify alternative local ports to map to the exposed container ports for HTTP and HTTPS using the `containerRunOpts` parameter.
    boolean skipDefaultPorts = false

    // If set to `true`, dev mode will not delete the temporary modified copy of your Containerfile/Dockerfile used to build the container image. 
    // This file is handy in case you need to debug the process of building the container image. 
    // The path of the temporary Containerfile/Dockerfile can be seen when dev mode displays the container `build` command. 
    // The default value is `false`. This attribute replaces the deprecated `keepTempDockerfile` attribute and will take precedence.
    boolean keepTempContainerfile = false

    // If set to `true`, change the action for running on demand tests from `Enter` to type `t` and press `Enter`. The default value is `false`.
    boolean changeOnDemandTestsAction = false

    //Docker aliases to maintain backwards compatability
    File dockerfile
    File dockerBuildContext
    String dockerRunOpts
    int dockerBuildTimeout
    boolean keepTempDockerfile = false
}