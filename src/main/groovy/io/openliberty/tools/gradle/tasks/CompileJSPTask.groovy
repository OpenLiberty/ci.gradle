/**
 * (C) Copyright IBM Corporation 2017, 2025.
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

import io.openliberty.tools.ant.jsp.CompileJSPs
import org.apache.tools.ant.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.War
import org.gradle.api.logging.LogLevel

import io.openliberty.tools.common.plugins.util.ServerFeatureUtil.FeaturesPlatforms

class CompileJSPTask extends AbstractFeatureTask {
    protected Project ant = new Project();

    CompileJSPTask() {
        configure({
            description = 'Compile the JSP files in the src/main/webapp directory. '
            group = 'Liberty'
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
        compileJsp.setTempdir(project.getLayout().getBuildDirectory().getAsFile().get())
        compileJsp.setDestdir(new File(project.getLayout().getBuildDirectory().getAsFile().get().getAbsolutePath()+"/classes/java"))
        compileJsp.setTimeout(project.liberty.jsp.jspCompileTimeout)
        // don't delete temporary server dir
        compileJsp.setCleanup(false)
        compileJsp.setProject(ant)
        Map<String, String> envVars = getToolchainEnvVar();
        if (!envVars.isEmpty()) {
            if (compileJsp.getEnvironmentVariables() != null && !compileJsp.getEnvironmentVariables().isEmpty()) {
                Map<String, String> mergedEnv = new HashMap<>(compileJsp.getEnvironmentVariables());
                mergedEnv.putAll(envVars);
                compileJsp.setEnvironmentVariables(mergedEnv);
            } else {
                compileJsp.setEnvironmentVariables(envVars);
            }
        }
        compileJsp.setTaskName('antlib:net/wasdev/wlp/ant:compileJSPs')
        War war;
        if(project.plugins.hasPlugin("war")){
            war = (War)project.war
            if ( war.getWebAppDirectory().getAsFile().get() != null) {
                compileJsp.setSrcdir( war.getWebAppDirectory().getAsFile().get())
            }
        }else {
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

        // Java version for compiling jsps
        setCompileJavaSourceVersion(compileJsp, task)

        //Feature list
        Set<String> installedFeatures = new HashSet<String>();
        FeaturesPlatforms fp = getSpecifiedFeatures(null);
        if (fp != null) {
            installedFeatures = fp.getFeatures();
        }

        //Set JSP Feature Version
        setJspVersion(compileJsp, installedFeatures);

        //Removing jsp and pages features as the jspVersion is already set at this point 
        Iterator<String> it = installedFeatures.iterator();
        while (it.hasNext()) {
            String nextItem = it.next();
            if (nextItem.startsWith("jsp-") || nextItem.startsWith("pages-")) {
                it.remove();
            }
        }
        
        if(installedFeatures != null && !installedFeatures.isEmpty()) {
            compileJsp.setFeatures(installedFeatures.toString().replace("[", "").replace("]", ""));
        }

        compileJsp.init()
        compileJsp.execute()
    }

    private void setCompileJavaSourceVersion(CompileJSPs compile, Task task) {
        Task compileTask = project.tasks.getByName('compileJava')
        
        if (compileTask != null) {
            String release = (String) compileTask.getOptions().getRelease().getOrNull()
            if (release != null) {
                logger.info("Found release from compileJava options: "+release)
                compile.setSource(release)
                return
            } 
        }
        
        if (project.hasProperty('sourceCompatibility')) {
            logger.info("Found sourceCompatibility")
            compile.setSource((String) project.getProperties().get('sourceCompatibility'))
        }
    }

    private void setJspVersion(CompileJSPs compile, Set<String> installedFeatures) {
        //If no conditions are met, defaults to 2.3 from the ant task
        if (project.liberty.jsp.jspVersion != null) {
            compile.setJspVersion(project.liberty.jsp.jspVersion);
        } else {
            Iterator it = installedFeatures.iterator();
            String currentFeature;
            while (it.hasNext()) {
                currentFeature = (String) it.next();
                if(currentFeature.startsWith("jsp-") || currentFeature.startsWith("pages-")) {
                    String version = currentFeature.substring(currentFeature.indexOf("-")+1);
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
