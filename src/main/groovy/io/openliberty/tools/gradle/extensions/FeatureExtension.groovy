/**
 * (C) Copyright IBM Corporation 2014, 2025.
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

class FeatureExtension {
    // Specifies the list of feature names to be installed. This can be a local ESA file, an IBM-Shortname or a Subsystem-SymbolicName of the Subsystem archive. 
    // If this list is not provided, the server must be created so that the configured feature list is used.
    List<String> name

    // Accept feature license terms and conditions. The default value is `false`, so you must add this property to get features installed if it is required. 
    boolean acceptLicense = false

    // Specify where to install the feature. The feature can be installed to any configured product extension location, or as a user feature (usr, extension). 
    // If this option is not specified the feature will be installed as a user feature. 
    String to

    // Specifies a single directory-based repository as the source of the assets. The default is to install from the online Liberty repository.
    String from

    // Specifies how features must be verified during a process or an installation. Supported values are `enforce`, `skip`, `all`, and `warn`. If this option is not specified, the default value is enforce.
    String verify = "enforce"
}
