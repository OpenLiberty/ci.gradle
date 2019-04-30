export testExitCode=0

./gradlew clean install check -Ptest.exclude="**/*15*" -Druntime=$RUNTIME -DruntimeVersion=$RUNTIME_VERSION --stacktrace --info --no-daemon
if [ $? ]; then
    testExitCode=1
fi

ls build/test-results/test/ >> out.txt
./gradlew wrapper --gradle-version 4.10

./gradlew clean install check -Ptest.include="**/*15*" -Druntime=$RUNTIME -DruntimeVersion=$RUNTIME_VERSION --stacktrace --info --no-daemon
if [ $? ]; then
    testExitCode=1
fi

ls build/test-results/test/ >> out.txt
cat out.txt

exit $testExitCode