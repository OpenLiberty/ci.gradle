/**
 * (C) Copyright IBM Corporation 2015, 2025.
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

class DeployExtension {
  
    // List of `war` and `ear` task objects used to create applications to copy to the `apps` folder. 
    // If no `apps` or `dropins` are configured and this project applies the `war` or `ear` plugin, the default application is installed using the `deploy` task. 
    // If the application is not configured in the server.xml file, application configuration is added to the `configDropins` folder.
    List<Object> apps

    // List of `war` or `ear` objects used to create applications to copy to the `dropins` folder.
    List<Object> dropins

    // The optional directory to which loose application dependencies referenced by the loose application configuration file are copied. 
    // For example, if you want loose application dependencies to be contained within the build directory, you could set this parameter to target. 
    // The loose application configuration file will reference this directory for the loose application dependencies instead of the local repository cache. 
    // Only applicable when `looseApplication` is true, which is the default. 
    File copyLibsDirectory
}


