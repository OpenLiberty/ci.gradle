/**
 * (C) Copyright IBM Corporation 2014, 2017.
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

import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.OutputDirectory
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.apache.commons.io.FilenameUtils
import org.w3c.dom.Element;
import java.util.regex.Pattern
import java.util.regex.Matcher
import java.text.MessageFormat

import org.gradle.api.Task
import net.wasdev.wlp.gradle.plugins.utils.*

import static net.wasdev.wlp.gradle.plugins.Liberty.TASK_CORE_EAR
import static net.wasdev.wlp.gradle.plugins.Liberty.TASK_CORE_WAR

abstract class InstallAppsTask extends AbstractServerTask {

  @OutputDirectory
  File appsDir() {
    Paths.get(getServerDir(project).absolutePath, DEPLOY_FOLDER_APPS).toFile()
  }

  @OutputDirectory
  File dropinsDir() {
    Paths.get(getServerDir(project).absolutePath, DEPLOY_FOLDER_DROPINS).toFile()
  }

    void installApps() {
        if ((server.apps == null || server.apps.isEmpty()) && (server.dropins == null || server.dropins.isEmpty())) {
            if (project.plugins.hasPlugin('war')) {
                server.apps = [project.war]
            }
        }
        if (server.apps != null && !server.apps.isEmpty()) {
            Tuple appsLists = splitAppList(server.apps)
            installMultipleApps(appsLists[0], 'apps')
            installFileList(appsLists[1], 'apps')
        }
        if (server.dropins != null && !server.dropins.isEmpty()) {
            Tuple dropinsLists = splitAppList(server.dropins)
            installMultipleApps(dropinsLists[0], 'dropins')
            installFileList(dropinsLists[1], 'dropins')
        }
    }

    private void installMultipleApps(List<Task> applications, String appsDir) {
        applications.each{ Task task ->
          installProject(task, appsDir)
        }
    }

    protected void installProject(Task task, String appsDir) throws Exception {
      if(isSupportedType()) {
        if(server.looseApplication){
          installLooseApplication(task, appsDir)
        } else {
          //installProjectArchive(task, appsDir)
        }
      } else {
        throw new GradleException(MessageFormat.format("Application {0} is not supported", task.archiveName))
      }
    }


    private boolean isSupportedType(){
      switch (getPackagingType()) {
        case TASK_CORE_EAR:
        case TASK_CORE_WAR:
            return true;
        default:
            return false;
        }
    }
    //Cleans up the application if the install style is switched from loose application to archive and vice versa
    protected void deleteApplication(File parent, File artifactFile) throws IOException {
        deleteApplication(parent, artifactFile.getName());
        if (artifactFile.getName().endsWith(".xml")) {
            deleteApplication(parent, artifactFile.getName().substring(0, artifactFile.getName().length() - 4));
        } else {
            deleteApplication(parent, artifactFile.getName() + ".xml");
        }
    }

    protected void deleteApplication(File parent, String filename) throws IOException {
        File application = new File(parent, filename);
        if (application.isDirectory()) {
            FileUtils.deleteDirectory(application);
        } else {
            application.delete();
        }
    }

    protected void installFromFile(File file, String appsDir) {
        Files.copy(file.toPath(), new File(getServerDir(project).toString() + '/' + appsDir + '/' + file.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    protected void installFileList(List<File> appFiles, String appsDir) {
        appFiles.each { File appFile ->
            installFromFile(appFile, appsDir)
        }
    }

    private Tuple splitAppList(List<Object> allApps) {
        List<File> appFiles = new ArrayList<File>()
        List<Task> appTasks = new ArrayList<Task>()

        allApps.each { Object appObj ->
            if (appObj instanceof Task) {
                appTasks.add((Task)appObj)
            } else if (appObj instanceof File) {
                appFiles.add((File)appObj)
            } else {
                logger.warn('Application ' + appObj.getClass.name + ' is expressed as ' + appObj.toString() + ' which is not a supported input type. Define applications using Task or File objects.')
            }
        }

        return new Tuple(appTasks, appFiles)
    }
}
