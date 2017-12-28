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

import net.wasdev.wlp.gradle.plugins.extensions.DeployExtension
import net.wasdev.wlp.gradle.plugins.extensions.LibertyExtension
import net.wasdev.wlp.gradle.plugins.utils.LibertyIntstallController
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class AbstractTask extends DefaultTask {

    //params that get built with installLiberty
    protected def params

    protected File getUserDir(Project project) {
        return getUserDir(project, LibertyIntstallController.getInstallDir(project))
    }

    protected File getUserDir(Project project, File installDir) {
        return (project.liberty.userDir == null) ? new File(installDir, 'usr') : new File(project.liberty.userDir)
    }

    protected File getOutputDir(Map<String, String> params) {
        if (params.get('outputDir') == null ) {
            return (params.get('outputDir'))
        } else {
            return (new File(params.get('outputDir')))
        }
    }

    protected boolean isLibertyInstalled(Project project) {
        File installDir = LibertyIntstallController.getInstallDir(project)
        return (installDir.exists() && new File(installDir, "lib/ws-launch.jar").exists())
    }

    Map<String, Object> loadPropertiesToMap(File propFile) {
        try {
            ResourceBundle resources = new PropertyResourceBundle(propFile.newDataInputStream())
            Map<String, Object> map = new HashMap<String, Object>()

            //convert ResourceBundle to Map
            Enumeration<String> keys = resources.getKeys()
            while (keys.hasMoreElements()) {
                String key = keys.nextElement()
                map.put(key, resources.getString(key))
            }
            return map
        } catch (FileNotFoundException e) {
            e.printStackTrace()
        } catch (IOException e) {
            e.printStackTrace()
        }
    }
}
