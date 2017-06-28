## Adding Tests
1) Create a new test class that extends `AbstractIntegrationTest` in `src/integTest/groovy/net/wasdev/wlp/gradle/plugins/`
2) Add a directory in `src/integTest/resources/` that contains the files needed to build the project you wish to test against
3) Below is and example test class outlining requirement for a new test suite 

```groovy
package net.wasdev.wlp.gradle.plugins;

import java.io.File

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

public class YourAwesomeTest extends AbstractIntegrationTest{
    // location of the directory containing the files need to run your test build
    // test resources are added in the root_dir/src/integTest/resources/ directory 
    static File sourceDir = new File("build/resources/integrationTest/your-awesome-build-files") // **required**
    // directory from which your build will take place
    // you only must configure the second parameter of the below file instantiation 
    static File buildDir = new File(integTestDir, "/your-awesome-test") // **required**
    // the name of the build file you are using in the root directory of the resources you are using
    static String buildFilename = "YourAwesomeTest.gradle" // **required**

    @BeforeClass 
    // this method configures the setup for your test build
    public static void setup() { // **required**
        // create the directory you will be building out of
        createDir(buildDir) // **required**
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
            // copy your testing resources into your build directory 
            createTestProject(buildDir, sourceDir, buildFilename) // **required**
        }
        else if(test_mode == "online"){
            // copy your testing resource into your build directory 
            createTestProject(buildDir, sourceDir, buildFilename) // **required**
        }
        // rename your build file to build.gradle so you can build with it...
        renameBuildFile(buildFilename, buildDir) // **required**
    }

    // Any configuration you need to run after your test suite
    // Hint: Stop any running servers here. Running servers may cause other test suites to fail
    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(buildDir, 'libertyStop')
    }
    
    @Test
    public void your_awesome_test() {
        try {
            runTasks(buildDir, 'yourAwesomeTask')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task yourAwesomeTask. "+ e)
        }
    }
}
```
