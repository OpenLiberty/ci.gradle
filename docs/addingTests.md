## Adding Tests
1) Create a new test class that extends `AbstractIntegrationTest` in `src/integTest/groovy/net/wasdev/wlp/gradle/plugins`
2) Add a directory in `src/integTest/resources` that contains the files needed to build the project you wish to test against
3) In your test class, add the `sourceDir` property to create a reference to your test resources. Example:

```groovy
static File sourceDir = new File("build/resources/integrationTest/liberty-test")
```

4) In your test class, add the following boilerplate code to your `@BeforeClass` method to ensure your test resources are loaded during test execution:

```groovy
    @BeforeClass
    public static void setup() {
        deleteDir(integTestDir)
        createDir(integTestDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
            createTestProject(integTestDir, sourceDir)
        }else if(test_mode == "online"){
            createTestProject(integTestDir, sourceDir)
        }
    }
```

5) Make sure your new test suite does not leave a server running. Below is an example of tear down logic that can be added to your `@AfterClass` to prevent a server from being left running:
```groovy
    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(integTestDir, 'libertyStop')
        deleteDir(integTestDir)
    }
```
