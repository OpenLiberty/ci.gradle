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

class UninstallFeature_multiple {
    def projectDir = new File('.')

    @Test
    public void test_uninstallFeature_multiple() {
        def file = new File(projectDir, "build/wlp/lib/features/com.ibm.websphere.appserver.mongodb-2.0.mf")
        def file2 = new File(projectDir, "build/wlp/lib/features/com.ibm.websphere.appserver.adminCenter-1.0.mf")

        assert !file.exists() : "com.ibm.websphere.appserver.mongodb-2.0.mf is not uninstalled"
        assert !file2.exists() : "com.ibm.websphere.appserver.adminCenter-1.0.mf is not installed"
    }
}
