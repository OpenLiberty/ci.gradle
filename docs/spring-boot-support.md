## Spring Boot Support

The springBootUtility command stores the dependent library JARs of the application to the target library cache and packages the remaining application artifacts into a thin application JAR. When you specify a read-only parent library cache, the command creates a target library cache that contains only the libraries that are not available in the parent cache. Use this capability to create efficient Docker layers for your Spring Boot application.

### Additional Parameters

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| sourceAppPath | The path of the source application file to thin. | Yes |
| parentLibCachePath | The directory path of the parent read-only library cache. The parent library cache is searched first to locate existing libraries. If the library is not found, the library is stored in the writable library cache that is specified by the `targetLibCachePath` option. If this option is not specified, no parent library cache is searched. | No |
| targetLibCachePath | The directory path that is used to save the library cache. If this option is not specified, a `lib.index.cache` directory is created in the parent directory of the source application. | No |
| targetThinAppPath | The path that is used to save the thin application file. If this option is not specified, a new file is created with the `.spring` extension in the parent directory of the source application. | No |
