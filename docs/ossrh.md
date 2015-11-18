
## Background

This project uses [Sonatype OSSRH (OSS Repository Hosting)][ossrh] for publishing snapshots and releases to the Maven Central Repository. See the [Gradle directions][ossrh-gradle] on details how to setup, configure, and publish Gradle artifacts.

## Configuration

### OSSRH and GPG credentials

In order to deploy snapshots and releases, you need to configure credential information for OSSRH and GPG signing in your `~/.gradle/gradle.properties` file:

```
ossrhUsername=your-jira-id
ossrhPassword=your-jira-password

signing.keyId=your-gpg-key-id
signing.password=your-gpg-password
signing.secretKeyRingFile=PathToYourKeyRingFile
```

See the [Working with PGP Signatures][pgp] page for detailed instructions on setting up and working with PGP keys. 

## Publishing snapshots

Publishing snapshots is very easy. Just execute:

```bash
$ gradle uploadArchives
```

## Creating and publishing releases

Use these steps to create and publish a release. 

1. On the `master` branch, update the `build.gradle` file with:
   * Set the `version` property to the right release version. For example: `1.2`. 
   * Set the `tag` property to `liberty-gradle-plugin-${version}`. For example: `liberty-gradle-plugin-1.2`.
   * Make sure that the version of dependencies match between the main `build.gradle` file and `src/integTest/resources/build.gradle` file.

2. Commit and push the changes to the `master` branch:
  ```bash
  $ git commit -m "prepare release liberty-gradle-plugin-${version}"
  $ git push origin master
  ```

3. Next, upload the release artifacts to the staging site:
  ```bash
  $ gradle uploadArchives
  ```

  Once this step is successful, the artifacts are uploaded to OSSRH. Next, do the following steps:
   1. Login into the [Sonatype Nexus Professional web interface][ossrh-web]
   2. Click on `Staging Repositories` and in the search text-box enter `wasdev`. One or more repositories should show up.
   3. Select the right repository and press on the `Close` button. Once the repository is closed (it might take a while), the `Summary` tab for the repository should contain an URL for the Maven repository that contains the published release artifacts. Use that URL to test the release artifacts.

4. Create a tag for the release:
  ```bash
  $ git tag liberty-gradle-plugin-${version}
  $ git push origin liberty-gradle-plugin-${version}
  ```

5. Prepare the `master` branch for the next development iteration by updating the `build.gradle` with:
 * Set the `version` property to the next version with `-SNAPSHOT` suffix. For example: `1.3-SNAPSHOT`. 
 * Set the `tag` property to `HEAD`.

6. Commit and push the changes to the `master` branch:
  ```bash
  $ git commit -m "prepare for next development iteration"
  $ git push origin master
  ```

7. Next, publish the new snaphost artifacts:
  ```bash
  $ gradle uploadArchives
  ```

### Promoting to Maven Central

Once testing of the release artifacts using the staging repository is successful, log back into the [Sonatype Nexus Professional web interface][ossrh-web], find the right repository, and press the `Release` button to promote the artifacts to the Maven Central Repository. The artifacts should become available in a few hours.

### Dropping the release

If testing was unsuccessful, log back into the [Sonatype Nexus Professional web interface][ossrh-web], find the right repository, and press the `Drop` button to remove the staged artifacts. After resolving the issues found with the release, start the release process over again.


[ossrh]: http://central.sonatype.org/pages/ossrh-guide.html
[ossrh-gradle]: http://central.sonatype.org/pages/gradle.html
[ossrh-web]: https://oss.sonatype.org/
[pgp]: http://central.sonatype.org/pages/working-with-pgp-signatures.html

