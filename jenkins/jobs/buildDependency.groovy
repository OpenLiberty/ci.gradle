pipelineJob("CiGradleDependencyBuild") {
   description("Builds ci.gradle dependency.")
   keepDependencies(false)
   definition {
      cpsScm {
         scm {
            git("https://github.com/OpenLiberty/ci.gradle.git", "main")
         }
         scriptPath("jenkins/buildDependency.jenkinsfile")
         lightweight()
      }
   }
   disabled(false)
   configure {
      it / 'properties' / 'com.sonyericsson.rebuild.RebuildSettings' {
         'autoRebuild'('false')
         'rebuildDisabled'('false')
      }
   }
}
