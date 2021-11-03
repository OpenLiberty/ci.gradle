## prepareFeature task
---
The `prepareFeature` task  generates `features.json` file for user features. The `features.json` file is JSON file that contains the information found within a feature's ESA manifest file. JSONs are a key requirement for the installation of any Liberty features(s) from a Maven repository. 


In Open Liberty and WebSphere Liberty runtime versions 21.0.0.11 and above, this task can prepare the user feature to generate the JSON file.


### Examples
1. Create a `features-bom` file for the user feature. The `features-bom` artifact in each groupId provides the bill of materials (BOM) for each Maven artifacts. 
 ```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>userTest.user.test.features</groupId>
  <artifactId>features-bom</artifactId>
  <version>1.0</version>
  <packaging>pom</packaging>
  <name>user feature bill of materials</name>
  <description>user feature bill of materials</description>
 
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>userTest.user.test.features</groupId>
        <artifactId>testesa1</artifactId>
        <version>1.0</version>
        <type>esa</type>
        <scope>provided</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>

 ```

2. Provide the maven coordinate of the custom made `features-bom` file:
 ```xml
apply plugin: 'liberty'

dependencies {
    featuresBom 'userTest.user.test.features:features-bom:1.0'
}
 ```
3. Install the user feature using the `installFeature` task.
