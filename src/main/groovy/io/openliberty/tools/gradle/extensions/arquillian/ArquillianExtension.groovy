/**
 * (C) Copyright IBM Corporation 2018, 2025.
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
package io.openliberty.tools.gradle.extensions.arquillian

class ArquillianExtension {
    //  Skips the `configureArquillian` task if `arquillian.xml` already exists in the `build` directory. Default is false.
    boolean skipIfArquillianXmlExists = false

    // Used to set key/value pairs of configuration parameters in `arquillian.xml`.
    // **Managed:** A dictionary containing values for `wlpHome`, `serverName`, and `httpPort` as specified in the `liberty-gradle-plugin`.
    // **Remote:** An empty dictionary when using the Arquillian Liberty Remote container.
    Map<String, String> arquillianProperties = null
}
