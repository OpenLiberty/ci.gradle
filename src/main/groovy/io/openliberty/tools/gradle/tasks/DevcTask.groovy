/**
 * (C) Copyright IBM Corporation 2020, 2025
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
package io.openliberty.tools.gradle.tasks

import org.gradle.api.tasks.TaskAction

class DevcTask extends DevTask {

    DevcTask() {
        configure({
            description = 'Runs a Liberty server in dev mode inside of a Docker container'
            group = 'Liberty'
        })
    }

    @TaskAction
    void action() {
        // set container variable for DevTask
        super.setContainer(true)
        
        // call dev mode
        super.action();
    }

}