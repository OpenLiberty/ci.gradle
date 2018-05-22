/**
 * (C) Copyright IBM Corporation 2017, 2018.
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

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;

import org.gradle.api.tasks.TaskAction
import org.gradle.api.Task
import org.gradle.api.GradleException
import org.gradle.api.tasks.bundling.War
import org.gradle.api.logging.LogLevel

import org.apache.tools.ant.Project;
import net.wasdev.wlp.ant.jsp.CompileJSPs;

class CompileJSPTask extends AbstractServerTask {
    protected Project ant = new Project();

    CompileJSPTask() {
        configure({
            description 'Compile the JSP files in the src/main/webapp directory. '
            logging.level = LogLevel.INFO
            group 'Liberty'
        })
    }

    @TaskAction
    protected void compileJSP(){
        if(getPackagingType().equals('war')){
            if ((server.apps == null || server.apps.isEmpty()) && (server.dropins == null || server.dropins.isEmpty())) {
                perTaskCompileJSP(project.war)
            }
            else if (server.apps != null && !server.apps.isEmpty()) {
                perTaskCompileJSP(server.apps[0])
            }
            else if (server.dropins != null && !server.dropins.isEmpty()) {
                perTaskCompileJSP(server.dropins[0])
            }
        }
    }

    protected void perTaskCompileJSP(Task task) throws Exception {
        CompileJSPs compileJsp = new CompileJSPs()
        compileJsp.setInstallDir(getInstallDir(project))
        compileJsp.setTempdir(project.buildDir)
        compileJsp.setDestdir(new File(project.buildDir.getAbsolutePath()+"/classes/java"))
        compileJsp.setTimeout(project.liberty.jspCompileTimeout)
        // don't delete temporary server dir
        compileJsp.setCleanup(false)
        compileJsp.setProject(ant)
        compileJsp.setTaskName('antlib:net/wasdev/wlp/ant:compileJSPs')

        if (project.convention.plugins.war.webAppDirName != null) {
            compileJsp.setSrcdir(project.convention.plugins.war.webAppDir)
        } else {
            compileJsp.setSrcdir(new File("src/main/webapp"))
        }
        Set<String> classpath = new HashSet<String>();

        // first add target/classes (or whatever is configured)
        classpath.add(getServerDir(project))
        for (File f : task.classpath) {
            classpath.add(f.getAbsolutePath())
        }
        setCompileDependencies(task, classpath)

        String classpathStr = join(classpath, File.pathSeparator);
        logger.debug("Classpath: " + classpathStr)
        compileJsp.setClasspath(classpathStr)

        if (project.liberty.jspVersion != null) {
            compileJsp.setJspVersion(project.liberty.jspVersion)
        }

        compileJsp.init()
        compileJsp.execute()
    }

    protected void setCompileDependencies(Task task, Set<String> classpaths) {
        ArrayList<File> deps = new ArrayList<File>();
        task.classpath.each {deps.add(it)}

        //Removes WEB-INF/lib/main directory since it is not a dependency
        if(deps != null && !deps.isEmpty()) {
            deps.remove(0)
        }

        for (File dep: deps) {
            if (dep != null) {
                if (!classpaths.add(dep.getAbsolutePath())) {
                    logger.debug("Duplicate dependency: " + dep.getName());
                }
            } else {
                logger.debug("Could not find: " + dep.getName());
            }
        }
    }

    protected String join(Set<String> depPathes, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String str : depPathes) {
            if (sb.length() != 0) {
                sb.append(sep);
            }
            sb.append(str);
        }
        return sb.toString();
    }
}
