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

class CleanExtension {
 
    // Delete all the files in the `${wlp_output_dir}/<server name>/logs` directory. The default value is `true`. 
    boolean logs = true

    // Delete all the files in the `${wlp_output_dir}/<server name>/workarea` directory. The default value is `true`. 
    boolean workarea = true

    // Delete all the files in the `${wlp_user_dir}/servers/<server name>/dropins` directory. The default value is `false`.
    boolean dropins = false

    // Delete all the files in the `${wlp_user_dir}/servers/<server name>/apps` directory. The default value is `false`.
    boolean apps = false
}


