/**
 * (C) Copyright IBM Corporation 2014, 2015.
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
package net.wasdev.wlp.gradle.plugins.tasks

import com.ibm.wsspi.kernel.embeddable.Server
import com.ibm.wsspi.kernel.embeddable.ServerBuilder
import com.ibm.wsspi.kernel.embeddable.Server.Result
import com.ibm.wsspi.kernel.embeddable.ServerEventListener
import com.ibm.wsspi.kernel.embeddable.ServerEventListener.ServerEvent
import com.ibm.wsspi.kernel.embeddable.ServerEventListener.ServerEvent.Type

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class RunTask extends AbstractTask {

    @TaskAction
    void run() {
        ServerBuilder builder = getServerBuilder(project);
        LibertyListener listener = new LibertyListener()
        builder.setServerEventListener(listener)
        Result result = builder.build().start().get()
        if (!result.successful()) throw result.getException()
    }

    protected ServerBuilder getServerBuilder(Project project) {
        ServerBuilder sb = new ServerBuilder()
        sb.setName(project.liberty.serverName)
        sb.setUserDir(getUserDir(project))
        if (project.liberty.outputDir != null) {
            sb.setOutputDir(new File(project.liberty.outputDir))
        }
        return sb
    }

    private static class LibertyListener implements ServerEventListener {

        private BlockingQueue<ServerEvent> queue = new LinkedBlockingQueue<ServerEvent>()

        void serverEvent(ServerEvent event) {
            queue.put(event)
        }

        ServerEvent next() {
            return queue.take()
        }

    }
}
