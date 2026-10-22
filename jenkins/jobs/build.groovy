pipelineJob("CiGradleBuild") {
   description("Builds ci.gradle.")
   keepDependencies(false)
   definition {
      cpsScm {
         scm {
            git("https://github.com/OpenLiberty/ci.gradle.git", "main")
         }
         scriptPath("jenkins/build.jenkinsfile")
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
