/**
 * (C) Copyright IBM Corporation 2017.
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

import org.apache.tools.ant.Project;
import net.wasdev.wlp.ant.jsp.CompileJSPs;

class CompileJSPTask extends AbstractServerTask {
  protected String jspVersion;
  protected int timeout = 30;
  protected Project ant = new Project();
  @TaskAction
  protected void compileJSP() throws Exception {

          CompileJSPs compileJsp = new CompileJSPs()
          compileJsp.setInstallDir(getInstallDir(project))
          compileJsp.setSrcdir(new File("src/main/webapp"))
          compileJsp.setTempdir(project.buildDir)
          compileJsp.setDestdir(getServerDir(project))
          compileJsp.setTimeout(timeout)
          // don't delete temporary server dir
          compileJsp.setCleanup(false)
          compileJsp.setProject(ant)
          compileJsp.setTaskName('antlib:net/wasdev/wlp/ant:compileJSPs')

          Set<String> classpath = new HashSet<String>();

          // first add target/classes (or whatever is configured)
          classpath.add(getServerDir(project))

          if(getPackagingType().equals('war')){
            if(project.sourceSets.main.getJava().getSourceDirectories().getSingleFile().exists())
              compileJsp.setSrcdir(project.sourceSets.main.getJava().getSourceDirectories().getSingleFile())

            if ((server.apps == null || server.apps.isEmpty()) && (server.dropins == null || server.dropins.isEmpty())) {
                server.apps = [project.war]
            }
            if (server.apps != null && !server.apps.isEmpty()) {
              setCompileDependencies(server.apps, classpath)
            }
            if (server.dropins != null && !server.dropins.isEmpty()) {
              setCompileDependencies(server.dropins, classpath)
            }
          }

          String classpathStr = join(classpath, File.pathSeparator);
          logger.debug("Classpath: " + classpathStr)
          compileJsp.setClasspath(classpathStr)

          // TODO should we try to calculate this from a pom dependency?
          if (jspVersion != null) {
              compileJsp.setJspVersion(jspVersion)
          }

          compileJsp.init()
          compileJsp.execute()
      }

      private void setCompileDependencies(List<Task> applications, Set<String> classpath) {
          applications.each{ Task task ->
            compileDependencyJSP(task, classpath)
          }
      }

      protected void compileDependencyJSP(Task task, Set<String> classpaths) {
        ArrayList<File> deps = new ArrayList<File>();
        task.classpath.each {deps.add(it)}

        //Removes WEB-INF/lib/main directory since it is not a dependency
        if(deps != null && !deps.isEmpty()){
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

      private String getPackagingType() throws Exception{
        if (project.plugins.hasPlugin("war") || !project.tasks.withType(War).isEmpty()) {
            return "war"
        }
        else if (project.plugins.hasPlugin("ear") || !project.tasks.withType(Ear).isEmpty()) {
            return "ear"
        }
        else {
            throw new GradleException("Archive path not found. Supported formats are jar, war, and ear.")
        }
    }

}
