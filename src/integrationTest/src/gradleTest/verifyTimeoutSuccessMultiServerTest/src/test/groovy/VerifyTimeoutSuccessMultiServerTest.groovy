/*
 * (C) Copyright IBM Corporation 2018.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.junit.Test

public class VerifyTimeoutSuccessMultiServerTest {
    def projectDir = new File('.')

    @Test
    public void test_multi_server_start_with_timeout_success() {
        assert new File(projectDir, 'build/wlp/usr/servers/libertyServer1/apps/sample.servlet-1.war').exists() : 'application not installed on server1'
        assert new File(projectDir, 'build/wlp/usr/servers/libertyServer2/apps/sample.servlet-1.war').exists() : 'application not installed on server2'
        assert new File(projectDir, 'build/wlp/usr/servers/libertyServer3/apps/sample.servlet-1.war').exists() : 'application not installed on server3'
    }
}
