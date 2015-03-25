/**
 * (C) Copyright IBM Corporation 2015.
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
package net.wasdev.wlp.gradle.plugins.tasks;

import org.gradle.api.DefaultTask
import org.gradle.api.Project

import com.ibm.wsspi.kernel.embeddable.Server
import com.ibm.wsspi.kernel.embeddable.ServerBuilder
import com.ibm.wsspi.kernel.embeddable.Server.Result
import com.ibm.wsspi.kernel.embeddable.ServerEventListener
import com.ibm.wsspi.kernel.embeddable.ServerEventListener.ServerEvent
import com.ibm.wsspi.kernel.embeddable.ServerEventListener.ServerEvent.Type

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

abstract class AbstractTask extends DefaultTask {
		
	protected void executeServerCommand(Project project, String command, Map<String, String> params) {
        project.ant.taskdef(name: 'server', 
                            classname: 'net.wasdev.wlp.ant.ServerTask', 
                            classpath: project.buildscript.configurations.classpath.asPath)
        params.put('operation', command)
        project.ant.server(params)
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


    protected Map<String, String> buildLibertyMap(Project project) {

        Map<String, String> result = new HashMap();
        result.put('serverName', project.liberty.serverName)
        def libertyUserDirFile = getUserDir(project)
        if (!libertyUserDirFile.isDirectory()) {
            libertyUserDirFile.mkdirs()
        }
        result.put('userDir', libertyUserDirFile)
        result.put('installDir', project.liberty.wlpDir)
        if (project.liberty.outputDir != null) {
            result.put('outputDir', project.liberty.outputDir)
        }          
        result.put('timeout', 300000)

        return result;
    }
    
    protected File getUserDir(Project project) {
        return (project.liberty.userDir == null) ? new File(project.liberty.wlpDir, 'usr') : new File(project.liberty.userDir)
    }

    protected static class LibertyListener implements ServerEventListener {

        protected BlockingQueue<ServerEvent> queue = new LinkedBlockingQueue<ServerEvent>()

        void serverEvent(ServerEvent event) {
            queue.put(event)
        }

        ServerEvent next() {
            return queue.take()
        }
    }
    
}