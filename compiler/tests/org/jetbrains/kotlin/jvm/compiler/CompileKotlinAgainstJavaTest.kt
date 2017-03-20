/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jvm.compiler

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.runner.RunWith

@SuppressWarnings("all")
@TestMetadata("compiler/testData/compileKotlinAgainstJava")
@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class CompileKotlinAgainstJavaTest : AbstractCompileJavaAgainstKotlinTest() {

    @TestMetadata("Interface.kt")
    fun testImplementsInterface() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/Interface.kt")
        doTest(fileName)
    }

    @TestMetadata("AbstractClass.kt")
    fun testExtendsAbstractClass() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/AbstractClass.kt")
        doTest(fileName)
    }

    @TestMetadata("Class.kt")
    fun testExtendsClass() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/Class.kt")
        doTest(fileName)
    }

    @TestMetadata("ListImpl.kt")
    fun testExtendsListImpl() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/ListImpl.kt")
        doTest(fileName)
    }

    @TestMetadata("CyclicDependencies.kt")
    fun testCyclicDependencies() {
        val fileName = KotlinTestUtils.navigationMetadata("compiler/testData/compileKotlinAgainstJava/CyclicDependencies.kt")
        doTest(fileName)
    }
}