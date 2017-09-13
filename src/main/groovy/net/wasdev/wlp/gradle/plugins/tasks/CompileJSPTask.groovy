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
import org.gradle.api.Project


import net.wasdev.wlp.ant.jsp.CompileJSPs;

class CompileJSPTask extends AbstractServerTask {

  protected String jspVersion;
  protected int timeout = 30;

  @TaskAction
  protected void compileJSP() throws Exception {
          CompileJSPs compile = (CompileJSPs) ant.createTask("antlib:net/wasdev/wlp/ant:compileJSPs");
          if (compile == null) {
              throw new IllegalStateException(MessageFormat.format(messages.getString("error.dependencies.not.found"), "compileJSPs"));
          }

          compile.setInstallDir(getInstallDir(project));

          compile.setSrcdir(new File("src/main/webapp"));
          compile.setDestdir(liberty.server.outputDir);
          compile.setTempdir(new File(project.buildDir));
          compile.setTimeout(timeout);

          // don't delete temporary server dir
          compile.setCleanup(false);

          List<Plugin> plugins = getProject().getBuildPlugins();
          for (Plugin plugin : plugins) {
              if ("org.apache.maven.plugins:maven-compiler-plugin".equals(plugin.getKey())) {
                  Object config = plugin.getConfiguration();
                  if (config instanceof Xpp3Dom) {
                      Xpp3Dom dom = (Xpp3Dom) config;
                      Xpp3Dom val = dom.getChild("source");
                      if (val != null) {
                          compile.setSource(val.getValue());
                      }
                  }
                  break;
              } else if ("org.apache.maven.plugins:maven-war-plugin".equals(plugin.getKey())) {
                  Object config = plugin.getConfiguration();
                  if (config instanceof Xpp3Dom) {
                      Xpp3Dom dom = (Xpp3Dom) config;
                      Xpp3Dom val = dom.getChild("warSourceDirectory");
                      if (val != null) {
                          compile.setSrcdir(new File(val.getValue()));
                      }
                  }
              }
          }

          Set<String> classpath = new TreeSet<String>();

          // first add target/classes (or whatever is configured)
          classpath.add(getProject().getBuild().getOutputDirectory());

          @SuppressWarnings("unchecked")
          Set<Artifact> dependencies = getProject().getArtifacts();
          for (Artifact dep : dependencies) {
              if (!dep.isResolved()) {
                  // TODO: Is transitive=true correct here?
                  dep = resolveArtifact(dep, true);
              }
              if (dep.getFile() != null) {
                  if (!classpath.add(dep.getFile().getAbsolutePath())) {
                      getLog().warn("Duplicate dependency: " + dep.getId());
                  }
              } else {
                  getLog().warn("Could not find: " + dep.getId());
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

      protected void init() throws GradleException {
          boolean doInstall = (installDirectory == null);

          super.init();

          if (doInstall) {
              try {
                  installServerAssembly();
              } catch (Exception e) {
                  throw new MojoExecutionException("Failure installing server", e);
              }
          }
      }

}
