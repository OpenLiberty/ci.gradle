package net.wasdev.wlp.gradle.plugins.tasks

import spock.lang.Specification

class InstallAppsSanityTaskTest extends Specification {

  def "check filename filter"(String testFilename, def expectedResult) {
    setup:
    def names = [new File(testFilename)]

    when:
    def results = InstallAppsSanityTask.filterBaseNames(names)

    then:
    assert results == expectedResult

    where:
    testFilename | expectedResult
    "sample.servlet-1.war" | ["sample.servlet-1"]
    "sample.servlet4-1.war" | ["sample.servlet4-1"]
    "sample.servlet-1.war.xml" | ["sample.servlet-1"]
    "sample.servlet4-1.war.xml" | ["sample.servlet4-1"]
    "sample.servlet4-1.ear" | ["sample.servlet4-1"]
    "sample.servlet-1.ear.xml" | ["sample.servlet-1"]
  }
}
