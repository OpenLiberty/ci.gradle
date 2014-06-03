/**
 * (C) Copyright IBM Corporation 2014.
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
package com.ibm.webshere.wlp.gradle.plugins

import org.gradle.api.*

import com.ibm.wsspi.kernel.embeddable.Server
import com.ibm.wsspi.kernel.embeddable.ServerBuilder
import com.ibm.wsspi.kernel.embeddable.Server.Result
import com.ibm.wsspi.kernel.embeddable.ServerEventListener
import com.ibm.wsspi.kernel.embeddable.ServerEventListener.ServerEvent
import com.ibm.wsspi.kernel.embeddable.ServerEventListener.ServerEvent.Type

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

import org.gradle.api.logging.LogLevel

class Liberty implements Plugin<Project> {

    void apply(Project project) {

        project.plugins.apply 'war'

        project.extensions.create('liberty', LibertyExtension)

        project.task('libertyRun') {
            description = "Runs a WebSphere Liberty Profile server under the Gradle process."
            doLast {
                ServerBuilder builder = getServerBuilder(project);

                LibertyListener listener = new LibertyListener()
                builder.setServerEventListener(listener)
                Result result = builder.build().start().get()
                if (!result.successful()) throw result.getException()

                while (!Type.STOPPED.equals(listener.next().getType())) {}
            }
        }

        project.task('libertyStatus') {
            description 'Checks the WebSphere Liberty Profile server is running.'
            logging.level = LogLevel.INFO
            doLast {
                try {
                    executeServerCommand(project, 'status', buildLibertyMap(project))
                } catch (Exception e) {
                    // Throws an exception if the server is stopped
                    println e
                }
            }
        }

        project.task('libertyCreate') {
            description 'Creates a WebSphere Liberty Profile server.'
            outputs.file { new File(getUserDir(project), "servers/${project.liberty.serverName}/server.xml") }
            logging.level = LogLevel.INFO
            doLast {
                executeServerCommand(project, 'create', buildLibertyMap(project))
            }
        }

        project.task('libertyStart') {
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            doLast {
                try {
                    executeServerCommand(project, 'start', buildLibertyMap(project))
                } catch (Exception e) {
                    // Throws an exception if the server is already started
                    println e
                }
            }
        }

        project.task('libertyStop') {
            description 'Stops the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            doLast {
                try {
                    executeServerCommand(project, 'stop', buildLibertyMap(project))
                } catch (Exception e) {
                    // Throws an exception if the server is already stopped
                    println e
                }
            }
        }
        project.tasks.clean.dependsOn project.tasks.libertyStop

        project.task('libertyPackage') {
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.INFO
            doLast {
                def params = buildLibertyMap(project);
                params.put('archive', new File(project.buildDir, project.liberty.serverName + '.zip'))
                executeServerCommand(project, 'package', params)
            }
        }

        project.task('deployWar') {
            description 'Deploys a WAR file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            doLast {
                def params = buildLibertyMap(project);
                params.put('file', project.war.archivePath)
                project.ant.taskdef(name: 'deploy', classname: 'com.ibm.websphere.wlp.ant.DeployTask') {
                    classpath {
                        fileset(dir: project.liberty.wlpDir + '/dev/tools/ant', includes: '*.jar')
                    }
                }
                project.ant.deploy(params)
            }
        }

        project.task('undeployWar') {
            description 'Removes a WAR file from the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            doLast {
                def params = buildLibertyMap(project)
                params.put('file', project.war.archivePath.name)
                project.ant.taskdef(name: 'undeploy', classname: 'com.ibm.websphere.wlp.ant.UndeployTask') {
                    classpath {
                        fileset(dir: project.liberty.wlpDir + '/dev/tools/ant', includes: '*.jar')
                    }
                }
                project.ant.undeploy(params)
            }
        }
        
    }

    private void executeServerCommand(Project project, String command, Map<String, String> params) {
        project.ant.taskdef(name: 'server', classname: 'com.ibm.websphere.wlp.ant.ServerTask') {
            classpath {
                fileset(dir: project.liberty.wlpDir + '/dev/tools/ant', includes: '*.jar')
            }
        }
        params.put('operation', command)
        project.ant.server(params)
    }

    private ServerBuilder getServerBuilder(Project project) {
        ServerBuilder sb = new ServerBuilder()
        sb.setName(project.liberty.serverName)
        sb.setUserDir(getUserDir(project))
        sb.setOutputDir(new File(project.liberty.outputDir))
        return sb
    }


    private Map<String, String> buildLibertyMap(Project project) {

        Map<String, String> result = new HashMap();
        result.put('serverName', project.liberty.serverName)
        def libertyUserDirFile = getUserDir(project)
        if (!libertyUserDirFile.isDirectory()) {
            libertyUserDirFile.mkdirs()
        }
        result.put('userDir', libertyUserDirFile)
        result.put('installDir', project.liberty.wlpDir)
        result.put('outputDir', project.liberty.outputDir)
        result.put('timeout', 300000)

        return result;
    }
    
    private File getUserDir(Project project) {
        return (project.liberty.userDir == null) ? new File(project.buildDir, 'wlp') : new File(project.liberty.userDir)
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
