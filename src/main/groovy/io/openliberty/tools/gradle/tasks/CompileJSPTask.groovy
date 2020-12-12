/**
 * (C) Copyright IBM Corporation 2017, 2020.
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
import io.openliberty.tools.ant.jsp.CompileJSPs;

class CompileJSPTask extends AbstractFeatureTask {
    protected Project ant = new Project();

    CompileJSPTask() {
        configure({
            description 'Compile the JSP files in the src/main/webapp directory. '
            group 'Liberty'
        })
    }

    @TaskAction
    protected void compileJSP(){
        if(getPackagingType().equals('war')){
            if ((server.deploy.apps == null || server.deploy.apps.isEmpty()) && (server.deploy.dropins == null || server.deploy.dropins.isEmpty())) {
                perTaskCompileJSP(project.war)
            }
            else if (server.deploy.apps != null && !server.deploy.apps.isEmpty()) {
                perTaskCompileJSP(server.deploy.apps[0])
            }
            else if (server.deploy.dropins != null && !server.deploy.dropins.isEmpty()) {
                perTaskCompileJSP(server.deploy.dropins[0])
            }
        }
    }

    protected void perTaskCompileJSP(Task task) throws Exception {
        CompileJSPs compileJsp = new CompileJSPs()
        compileJsp.setInstallDir(getInstallDir(project))
        compileJsp.setTempdir(project.buildDir)
        compileJsp.setDestdir(new File(project.buildDir.getAbsolutePath()+"/classes/java"))
        compileJsp.setTimeout(project.liberty.jsp.jspCompileTimeout)
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

        //Feature list
        Set<String> installedFeatures = getSpecifiedFeatures(null);

        //Set JSP Feature Version
        setJspVersion(compileJsp, installedFeatures);

        //Removing jsp features at it is already set at this point 
        installedFeatures.remove("jsp-2.3");
        installedFeatures.remove("jsp-2.2");
        
        if(installedFeatures != null && !installedFeatures.isEmpty()) {
            compileJsp.setFeatures(installedFeatures.toString().replace("[", "").replace("]", ""));
        }

        compileJsp.init()
        compileJsp.execute()
    }

        private void setJspVersion(CompileJSPs compile, Set<String> installedFeatures) {
            //If no conditions are met, defaults to 2.3 from the ant task
            if (project.liberty.jsp.jspVersion != null) {
                compile.setJspVersion(project.liberty.jsp.jspVersion);
            }
            else {
                Iterator it = installedFeatures.iterator();
                String currentFeature;
                while (it.hasNext()) {
                    currentFeature = (String) it.next();
                    if(currentFeature.startsWith("jsp-")) {
                        String version = currentFeature.replace("jsp-", "");
                        compile.setJspVersion(version);
                        break;
                    }
                }
            }
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
