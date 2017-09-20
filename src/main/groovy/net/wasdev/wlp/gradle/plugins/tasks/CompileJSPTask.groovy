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

import org.gradle.api.tasks.TaskAction
import org.gradle.api.Task

import net.wasdev.wlp.ant.jsp.CompileJSPs;

class CompileJSPTask extends AbstractServerTask {

  protected String jspVersion;
  protected int timeout = 30;

  @TaskAction
  protected void compileJsps() throws Exception {
          CompileJSPs compile = (CompileJSPs) ant.createTask("antlib:net/wasdev/wlp/ant:compileJSPs");
          if (compile == null) {
              throw new IllegalStateException(MessageFormat.format(messages.getString("error.dependencies.not.found"), "compileJSPs"));
          }

          compile.setInstallDir(getInstallDir(project));

          compile.setSrcdir(new File("src/main/webapp"));
          compile.setDestdir(getOutputDir(params));
          compile.setTempdir(new File(project.buildDir));
          compile.setTimeout(timeout);

          // don't delete temporary server dir
          compile.setCleanup(false);

          Set<String> classpath = new TreeSet<String>();

          // first add target/classes (or whatever is configured)
          classpath.add(getOutputDir(params))

          if(getPackagingType().equals('war')){
            if(project.war.source.getSingleFile().exists())
              compile.setSrcdir = project.war.source.getSingleFile().getAbsolutePath()

            if ((server.apps == null || server.apps.isEmpty()) && (server.dropins == null || server.dropins.isEmpty())) {
                server.apps = [project.war]
            }
            if (server.apps != null && !server.apps.isEmpty()) {
              setCompileDependencies(server.apps, compile, classpath)
            }
            if (server.dropins != null && !server.dropins.isEmpty()) {
              setCompileDependencies(server.dropins, compile, classpath)
            }
          }

          String classpathStr = join(classpath, File.pathSeparator);
          log.debug("Classpath: " + classpathStr);
          compile.setClasspath(classpathStr);

          // TODO should we try to calculate this from a pom dependency?
          if (jspVersion != null) {
              compile.setJspVersion(jspVersion);
          }

          // TODO do we need to add features?
          compile.execute();
      }

      private void setCompileDependencies(List<Task> applications, CompileJSPs compile, Set<String> classpath) {
          applications.each{ Task task ->
            compileDependencyJSP(task, compile, classpath)
          }
      }

      private void compileDependencyJSP(Task task, CompileJSPs compile, Set<String> classpath) {
        ArrayList<File> deps = new ArrayList<File>();
        task.classpath.each {deps.add(it)}
        //Removes WEB-INF/lib/main directory since it is not rquired in the xml
        if(deps != null && !deps.isEmpty()){
          deps.remove(0)
        }

        for (File dep: deps) {
          if(!projectPath.isEmpty() && project.getRootProject().findProject(projectPath) != null){
            if (dep.getFile() != null) {
                if (!classpath.add(dep.getFile().getAbsolutePath())) {
                    getLog().warn("Duplicate dependency: " + dep.getId());
                }
            } else {
                getLog().warn("Could not find: " + dep.getId());
            }
          }
        }
      }

      private String join(Set<String> depPathes, String sep) {
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
