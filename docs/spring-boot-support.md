## Spring Boot Support

The `liberty-gradle-plugin` provides support for Spring Boot applications, allowing you to thin out and install Spring Boot executable JARs to Open Liberty and WebSphere Liberty runtime versions 18.0.0.2 and above. You can [package executable JAR or WAR archives](https://docs.spring.io/spring-boot/docs/2.3.3.RELEASE/gradle-plugin/reference/html/#packaging-executable) using `spring-boot-dependencies`.

### Additional Parameters

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| sourceAppPath | The path of the source application file to thin. | Yes |
| parentLibCachePath | The directory path of the parent read-only library cache. The parent library cache is searched first to locate existing libraries. If the library is not found, the library is stored in the writable library cache that is specified by the `targetLibCachePath` option. If this option is not specified, no parent library cache is searched. | No |
| targetLibCachePath | The directory path that is used to save the library cache. If this option is not specified, a `lib.index.cache` directory is created in the parent directory of the source application. | No |
| targetThinAppPath | The path that is used to save the thin application file. If this option is not specified, a new file is created with the `.spring` extension in the parent directory of the source application. | No |
  
  
The `server.xml` provided by the `serverXml` parameter should enable the one of the following Spring Boot features.

| Feature | Description |
| ------- | ----------- |
| springBoot-1.5 | Required to support applications with Spring Boot version 1.5.x. |
| springBoot-2.0 | Required to support applications with Spring Boot version 2.0.x and above. |

The Liberty features that support the Spring Boot starters can be found [here](https://www.ibm.com/support/knowledgecenter/SSAW57_liberty/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/rwlp_springboot.html). They should be enabled in the `server.xml` along with the appropriate Spring Boot feature.
