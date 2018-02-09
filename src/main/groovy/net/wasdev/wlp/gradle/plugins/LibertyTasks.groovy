package net.wasdev.wlp.gradle.plugins

import net.wasdev.wlp.gradle.plugins.extensions.ServerExtension

import org.gradle.api.Project

abstract class LibertyTasks {
    Project project

    LibertyTasks (Project project) {
        this.project = project
    }

    abstract void applyTasks()

    protected List<String> installDependsOn(ServerExtension server, String elseDepends) {
        List<String> tasks = new ArrayList<String>()
        boolean apps = dependsOnApps(server)
        boolean feature = dependsOnFeature(server)

        if (apps) tasks.add('installApps')
        if (feature) tasks.add('installFeature')
        if (!apps && !feature) tasks.add(elseDepends)
        return tasks
    }

    protected boolean dependsOnApps(ServerExtension server) {
        return ((server.apps != null && !server.apps.isEmpty()) ||
                (server.dropins != null && !server.dropins.isEmpty()))
    }

    protected boolean dependsOnFeature(ServerExtension server) {
        return (server.features.name != null && !server.features.name.isEmpty())
    }

    public void checkServerEnvProperties(ServerExtension server) {
        if (server.outputDir == null) {
            Properties envProperties = new Properties()
            //check server.env files and set liberty.server.outputDir
            if (server.configDirectory != null) {
                File serverEnvFile = new File(server.configDirectory, 'server.env')
                if (serverEnvFile.exists()) {
                    serverEnvFile.text = serverEnvFile.text.replace("\\", "/")
                    envProperties.load(new FileInputStream(serverEnvFile))
                    setServerOutputDir(server, (String) envProperties.get("WLP_OUTPUT_DIR"))
                }
            } else if (server.serverEnv.exists()) {
                server.serverEnv.text = server.serverEnv.text.replace("\\", "/")
                envProperties.load(new FileInputStream(server.serverEnv))
                setServerOutputDir(server, (String) envProperties.get("WLP_OUTPUT_DIR"))
            }
        }
    }

    private void setServerOutputDir(ServerExtension server, String envOutputDir){
        if (envOutputDir != null) {
            server.outputDir = envOutputDir
        }
    }
}
