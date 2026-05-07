## libertyPrepareConfig task

---

Prepare Liberty configuration and generate `liberty-plugin-config.xml` with a mock Liberty server structure. This lightweight task evaluates project configuration, creates a temporary Liberty server structure in a configurable temporary directory, copies all configuration files, and generates metadata needed by IDE tools and language servers.

This task is particularly useful for:
- Enabling IDE support for Liberty configuration files (server.xml, bootstrap.properties, server.env)
- Providing configuration metadata to language servers for code completion and diagnostics
- Supporting Liberty Tools and other IDE extensions
- Quick configuration validation without full project build
- Creating a mock Liberty server structure for language server integration

**What this task does:**
1. Creates a mock Liberty server structure in `build/tmp/liberty-var-cache/wlp/usr/servers/{serverName}/` (configurable)
2. Copies all configuration files (server.xml, bootstrap.properties, server.env, jvm.options, etc.) to the mock server
3. Generates `liberty-plugin-config.xml` pointing to the mock server structure

The task does NOT install Liberty runtime. It only creates a minimal directory structure that mimics a Liberty server and copies configuration files.

---

### Usage

The `libertyPrepareConfig` task is typically used in IDE scenarios where you need configuration metadata before building the project:

```bash
gradle libertyPrepareConfig
```

---

### Configuration

The `libertyPrepareConfig` task uses the standard Liberty server configuration from the `liberty` extension. See [Liberty extension properties](libertyExtensions.md#liberty-extension-properties) for available configuration options.

#### Configuration Parameters

| Parameter | Description | Required | Default |
| --------- | ----------- | -------- | ------- |
| prepareConfigTempDir | Name of the temporary directory used for mock Liberty server structures. This directory is created under the build output directory (`build/`). | No | `tmp/liberty-var-cache` |

Example configuration:

```groovy
liberty {
    server {
        name = 'myServer'
        configDirectory = file("${project.rootDir}/src/main/liberty/config")
        prepareConfigTempDir = 'my-temp-dir'  // Optional: customize temp directory
    }
}
```

---

### Examples

#### Example 1: Basic usage

Generate configuration metadata with server information:

```bash
gradle libertyPrepareConfig
```

This will create `build/liberty-plugin-config.xml` with project metadata, dependencies, and configuration file references.

#### Example 2: Custom temporary directory

Use a custom temporary directory name:

```bash
gradle libertyPrepareConfig -PprepareConfigTempDir=my-temp-dir
```

Or configure it in `build.gradle`:

```groovy
liberty {
    server {
        prepareConfigTempDir = 'my-temp-dir'
    }
}
```

This will create the mock server structure in `build/my-temp-dir/wlp/usr/servers/{serverName}/` instead of the default location.

#### Example 3: IDE integration

Configure the task to run automatically during project initialization by adding it as a dependency to another task:

```groovy
tasks.named('compileJava') {
    dependsOn 'libertyPrepareConfig'
}
```

#### Example 4: Custom server configuration

```groovy
liberty {
    server {
        name = 'testServer'
        configDirectory = file("${project.rootDir}/config")
        bootstrapPropertiesFile = file("${project.rootDir}/config/bootstrap.properties")
        jvmOptionsFile = file("${project.rootDir}/config/jvm.options")
        serverEnvFile = file("${project.rootDir}/config/server.env")
    }
}
```

Then run:

```bash
gradle libertyPrepareConfig
```

---

### Generated Configuration File and Mock Server Structure

The `libertyPrepareConfig` task generates:

1. **Mock Liberty Server Structure** in `build/tmp/liberty-var-cache/` (or custom directory):
   ```
   build/tmp/liberty-var-cache/
   └── wlp/
       └── usr/
           └── servers/
               └── {serverName}/
                   ├── server.xml
                   ├── bootstrap.properties
                   ├── server.env
                   ├── jvm.options
                   └── (other config files)
   ```

2. **Configuration Metadata File** `build/liberty-plugin-config.xml` containing:

- Install directory (points to `build/tmp/liberty-var-cache/wlp`)
- User directory (points to `build/tmp/liberty-var-cache/wlp/usr`)
- Server directory (points to `build/tmp/liberty-var-cache/wlp/usr/servers/{serverName}`)
- Server name and output directory paths
- Project type (packaging)
- Project compile dependencies
- Configuration directory
- Server configuration file path (in mock server)
- Bootstrap properties file path (in mock server)
- JVM options file path (in mock server)
- Server environment file path (in mock server)
- Applications directory (`apps` or `dropins`)
- Loose application configuration
- Strip version settings
- Application filename

---

### Use Cases

#### 1. IDE Language Server Support

IDEs using Liberty language servers can invoke this task to get configuration metadata:

```bash
gradle libertyPrepareConfig
```

The generated `liberty-plugin-config.xml` provides language servers with information needed to offer:
- Code completion for Liberty configuration
- Validation of server configuration
- Quick fixes and diagnostics
- Custom file path resolution (for non-standard locations)

**For full variable resolution features**, first install Liberty using `libertyCreate`:

```bash
gradle libertyCreate
gradle libertyPrepareConfig
```

#### 2. CI/CD Pipeline Validation

Validate Liberty configuration early in the pipeline without full build:

```bash
gradle libertyPrepareConfig
# Parse and validate liberty-plugin-config.xml
```

#### 3. Multi-Module Project Setup

For multi-module projects, run at the parent level to prepare configuration for all modules:

```bash
gradle :module-name:libertyPrepareConfig
```

#### 4. Pre-Build Configuration Analysis

Analyze project configuration before committing to a full build:

```bash
gradle libertyPrepareConfig
# Analyze build/liberty-plugin-config.xml for issues
```

---

### Comparison with Other Tasks

| Task | Liberty Install | Server Creation | Mock Server Structure | Config Files Copied | Use Case |
|------|----------------|-----------------|----------------------|---------------------|----------|
| `libertyPrepareConfig` | No | No | Yes (in tmp/liberty-var-cache) | Yes (to mock server) | Generate config metadata and mock structure for tools |
| `libertyCreate` | Yes | Yes | No | Yes (to real server) | Create and configure Liberty server |
| `installLiberty` | Yes | No | No | No | Install Liberty runtime only |
| `libertyDev` | Yes | Yes | No | Yes (to real server) | Development mode with hot reload |

---

### Performance Considerations

The `libertyPrepareConfig` task is designed to be lightweight and fast:

- **Typical execution time**: ~1-2 seconds
- **No Liberty download**: Does not download or install Liberty
- **No server creation**: Does not create real server directories
- **Minimal I/O**: Only reads Gradle configuration and writes one XML file

This makes it ideal for IDE integration where responsiveness is critical.

---

### Language Server Integration

The `libertyPrepareConfig` task is designed to work with two Liberty language servers:

#### 1. lemminx-liberty (XML Language Server)
Provides features for `server.xml` and related XML configuration files:
- Feature completion and validation
- Configuration element completion
- Variable resolution (requires Liberty installation)
- Hover documentation

#### 2. liberty-ls (Properties Language Server)
Provides features for `bootstrap.properties` and `server.env` files:
- Property name completion
- Property value validation
- Custom file path detection

**Note**: For full variable resolution in `server.xml`, Liberty must be installed first using `libertyCreate` or `libertyDev`. The `libertyPrepareConfig` task will include the Liberty installation paths in the generated config if Liberty is already present.

---

### Troubleshooting

#### Configuration file not generated

Check that the task executed successfully:

```bash
gradle libertyPrepareConfig --info
```

The output will show the exact location where the file is being written.

#### Missing server information

Ensure your Liberty server configuration is properly set in the `build.gradle` file:

```groovy
liberty {
    server {
        name = 'myServer'
        configDirectory = file("${project.rootDir}/src/main/liberty/config")
    }
}
```

#### Variable resolution not working in IDE

Variable resolution requires Liberty to be installed. Run:

```bash
gradle libertyCreate
gradle libertyPrepareConfig
```

This will install Liberty and generate the config file with Liberty installation paths.

---

### See Also

- [Liberty Extension Properties](libertyExtensions.md#liberty-extension-properties)
- [libertyCreate task](libertyCreate.md) - Install Liberty and create server
- [installLiberty task](installLiberty.md) - Install Liberty runtime only
- [libertyDev task](libertyDev.md) - Development mode with hot reload