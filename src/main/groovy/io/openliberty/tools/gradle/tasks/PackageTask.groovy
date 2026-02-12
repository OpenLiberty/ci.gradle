/**
 * (C) Copyright IBM Corporation 2014, 2025.
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

import io.openliberty.tools.common.plugins.util.InstallFeatureUtil
import io.openliberty.tools.common.plugins.util.VersionUtility
import org.gradle.api.GradleException
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class PackageTask extends AbstractServerTask {
    public static final String MIN_SUPPORTED_VERSION_WITH_ARCHIVE_OPTION_POSIX_FORMAT = "25.0.0.11";
    private enum PackageFileType {
        JAR("jar"),
        TAR("tar"),
        TARGZ("tar.gz"),
        ZIP("zip");

        private final String value;

        private PackageFileType(final String val) {
            this.value = val;
        }

        private static final Map<String, PackageFileType> lookup = new HashMap<String, PackageFileType>();

        static {
            for (PackageFileType s : EnumSet.allOf(PackageFileType.class)) {
               lookup.put(s.value, s);
            }
        }

        public static PackageFileType getPackageFileType(String input) {
            return lookup.get(input);
        } 

        public String getValue() {
            return this.value;
        }
    }

    PackageTask() {
        configure({
            description = 'Generates a Liberty server archive.'
            group = 'Liberty'
        })
    }

    @TaskAction
    void packageServer() {
        // Set default server.outputDir to liberty-alt-output-dir for libertyPackage task.
        if (getOutputDir(project).equals(getUserDir(project).toString() + "/servers")) {
            server.outputDir = new File(project.getLayout().getBuildDirectory().getAsFile().get(), "liberty-alt-output-dir");
        }

        def params = buildLibertyMap(project)
        
        def packageFile = getPackageFile()
        params.put('archive', packageFile)
        logger.info 'Packaging ' + packageFile

        List<InstallFeatureUtil.ProductProperties> propertiesList = InstallFeatureUtil.loadProperties(getInstallDir(project));
        String openLibertyVersion = InstallFeatureUtil.getOpenLibertyVersion(propertiesList);
        if (openLibertyVersion != null &&
                VersionUtility.compareArtifactVersion(openLibertyVersion,
                        MIN_SUPPORTED_VERSION_WITH_ARCHIVE_OPTION_POSIX_FORMAT, true) >= 0) {
            params.put('usePosixRules', "true")
        }

        if (server.packageLiberty.include != null && server.packageLiberty.include.length() != 0) {
            params.put('include', server.packageLiberty.include)
        }
        if (server.packageLiberty.serverRoot != null && server.packageLiberty.serverRoot.length() != 0) {
            params.put('serverRoot', server.packageLiberty.serverRoot)
        }
        if (server.packageLiberty.os != null && server.packageLiberty.os.length() != 0) {
            params.put('os', server.packageLiberty.os)
        }

        executeServerCommand(project, 'package', params)
    }

    @OutputFile
    public File getPackageFile() throws IOException {
        return new File(getPackageDirectory(), getPackageName() + "." + getPackageFileType().getValue())
    }

    private static void createDir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new GradleException("Unable to create directory '$dir.canonicalPath'.")
            }
        }
    }

    /**
     * Returns package name
     * 
     * @return specified package name, or default ${project.name} if unspecified
     */
    private String getPackageName() {
        if (server.packageLiberty.packageName != null && !server.packageLiberty.packageName.isEmpty()) {
            return server.packageLiberty.packageName
        }
        return project.getName()
    }

    /**
     * Returns canonical path to package directory
     * 
     * @return canonical path to specified package directory, or default ${project.buildDir}/libs if unspecified
     * @throws IOException
     */
    private String getPackageDirectory() throws IOException {
        def buildDirLibFolder = new File(project.getLayout().getBuildDirectory().getAsFile().get(),'libs')
        if (server.packageLiberty.packageDirectory != null && !server.packageLiberty.packageDirectory.isEmpty()) {
            // check if path is relative or absolute, convert to canonical
            def dir = new File(server.packageLiberty.packageDirectory)
            if (dir.isAbsolute()) {
                createDir(dir)
                return dir.getCanonicalPath()
            } else { //relative path
                // default to ${project.buildDir}/libs for containing folder
                def packageDir = new File(buildDirLibFolder, server.packageLiberty.packageDirectory)
                createDir(packageDir)
                return packageDir.getCanonicalPath()
            }
        } else {
            // default to ${project.buildDir}/libs
            createDir(buildDirLibFolder)
            return buildDirLibFolder.getCanonicalPath()
        }
    }

    private ArrayList<String> parseInclude() {
        ArrayList<String> includeValues
        List<String> includeStrings
        def includeValue = server.packageLiberty.include

        if (includeValue != null && !includeValue.isEmpty()) {
            def includeTrim = includeValue.trim()
            includeStrings = Arrays.asList(includeTrim.split(","))
            includeValues = new ArrayList<String>(includeStrings)
            for (int i = 0; i < includeValues.size(); i++) {
                String value = includeValues.get(i)
                if (value.trim().length() > 0) {
                    includeValues.set(i, value.trim())
                }
            }
        } else {
            includeValues = new ArrayList<String>()
        }
        return includeValues
    }

    /**
     * Returns PackageFileType for specified packageType and include values. If packageType is not specified, 
     * and include contains `runnable`, default to PackageFileType.JAR. Otherwise, default 
     * to PackageFileType.ZIP. If packageType is specified, and include contains `runnable`, 
     * then packageType must be `jar`.
     * 
     */
    private PackageFileType getPackageFileType() {
        ArrayList<String> includeValues = parseInclude()
        def packageFileType = PackageFileType.ZIP
        if (server.packageLiberty.packageType == null) {
            if (includeValues.contains("runnable")) {
                logger.debug("Defaulting `packageType` to `jar` because the `include` value contains `runnable`.")
                packageFileType = PackageFileType.JAR
            } else {
                logger.debug("Defaulting `packageType` to `zip`.")
                packageFileType = PackageFileType.ZIP
            }
        } else {
            PackageFileType packType = PackageFileType.getPackageFileType(server.packageLiberty.packageType)
            if (packType != null) {
                // if include contains runnable, validate packageType
                if (includeValues.contains("runnable") && packType != PackageFileType.JAR) {
                    logger.debug("The `packageType` value " + server.packageLiberty.packageType + " is not supported when the `include` value contains `runnable`. Defaulting to 'jar'.")
                    packageFileType = PackageFileType.JAR
                } else {
                    packageFileType = packType
                }
            } else {
                logger.debug("The `packageType` value " + server.packageLiberty.packageType + " is not supported. Defaulting to 'zip'.")
                packageFileType = PackageFileType.ZIP
            }
        }
        return packageFileType
    }


}
