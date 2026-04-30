/**
 * (C) Copyright IBM Corporation 2019, 2024.
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
package io.openliberty.tools.gradle.utils

import io.openliberty.tools.common.CommonLoggerI

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

public class CommonLogger implements CommonLoggerI {

    private Project project

    public CommonLogger(Project project) {
        this.project = project
    }

    @Override
    public void debug(String msg) {
        project.getLogger().debug(msg)
    }

    @Override
    public void debug(String msg, Throwable e) {
        project.getLogger().debug(msg, (Throwable) e)
    }

    @Override
    public void debug(Throwable e) {
        project.getLogger().debug("Exception received: "+e.getMessage(), (Throwable) e)
    }

    @Override
    public void warn(String msg) {
        project.getLogger().warn(msg)
    }

    @Override
    public void info(String msg) {
        project.getLogger().info(msg)
    }

    @Override
    public void error(String msg) {
        project.getLogger().error(msg)
    }

    @Override
    public boolean isDebugEnabled() {
        return project.getLogger().isEnabled(LogLevel.DEBUG)
    }

}