## libertyPrepareConfig task

---

Prepare Liberty configuration and generate `liberty-plugin-config.xml` with a mock Liberty server structure. This lightweight task creates a temporary Liberty server structure, copies configuration files, and generates metadata needed by IDE tools and language servers.

**What this task does:**
1. Creates a mock Liberty server structure in `build/.libertyls-var-cache/wlp/usr/servers/{serverName}/` (configurable)
2. Copies all configuration files (server.xml, bootstrap.properties, server.env, jvm.options, etc.) to the mock server
3. Generates `liberty-plugin-config.xml` pointing to the mock server structure

**Note:** This task does NOT install Liberty runtime. It only creates a minimal directory structure and copies configuration files.

---

### Usage

Run directly from the command line:

```bash
gradle libertyPrepareConfig
```

---

### Configuration

The `libertyPrepareConfig` task uses the standard Liberty server configuration from the `liberty` extension. See [Liberty extension properties](libertyExtensions.md#liberty-extension-properties) for available configuration options.

The temporary directory for the mock server structure defaults to `.libertyls-var-cache` (a hidden directory). To override this, use a project property:

```bash
gradle libertyPrepareConfig -PprepareConfigTempDir=my-temp-dir
```

Or set it in `gradle.properties`:

```properties
prepareConfigTempDir=my-temp-dir
```

This will create the mock server structure in `build/my-temp-dir/wlp/usr/servers/{serverName}/` instead of the default location.

---

### Generated Files

The task generates:

1. **Mock Liberty Server Structure** in `build/.libertyls-var-cache/`:
   ```
   build/.libertyls-var-cache/
   └── wlp/
       └── usr/
           └── servers/
               └── {serverName}/
                   ├── server.xml
                   ├── bootstrap.properties
                   ├── server.env
                   └── jvm.options
   ```

2. **Configuration Metadata File** `build/liberty-plugin-config.xml` containing project metadata, dependencies, and configuration file paths for IDE tools and language servers.

---

### Use Cases

- **IDE Language Server Support**: Provides metadata for code completion, validation, and diagnostics in Liberty configuration files
- **Quick Configuration Validation**: Validate configuration without full project build
- **CI/CD Integration**: Generate configuration metadata early in the pipeline

**For full variable resolution features**, install Liberty first:

```bash
gradle libertyCreate
gradle libertyPrepareConfig
```

---

### See Also

- [Liberty Extension Properties](libertyExtensions.md#liberty-extension-properties)
- [libertyCreate task](libertyCreate.md) - Install Liberty and create server
- [libertyDev task](libertyDev.md) - Development mode with hot reload