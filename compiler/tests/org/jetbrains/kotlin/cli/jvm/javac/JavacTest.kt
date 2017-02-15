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

package org.jetbrains.kotlin.cli.jvm.javac

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.ExtendedJavac
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.io.File
import java.net.URI
import java.util.regex.Pattern
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

class JavacTest : KtUsefulTestCase() {

    fun testCommon() {
        ExtendedJavac.getTrees(listOf(KotlinFileObject4()))
    }

    private fun getAllJavaFilesFromDir(dir: String): List<File> {
        val folder = File(dir).check { it.isDirectory } ?: return emptyList()
        val collectedFiles = arrayListOf<File>()

        FileUtil.collectMatchedFiles(folder, Pattern.compile(".*.java"), collectedFiles)

        return collectedFiles
    }

}

private class KotlinFileObject : SimpleJavaFileObject(URI("pack/SomeClass.java"), JavaFileObject.Kind.SOURCE) {

    override fun getCharContent(ignoreEncodingErrors: Boolean) =
            "package pack; public class SomeClass { public static void func() { System.out.println(); } " +
            "/*public static SomeClass2 func2() { return new SomeClass2(); }*/ }"
}

private class KotlinFileObject2 : SimpleJavaFileObject(URI("pack/Singleton.java"), JavaFileObject.Kind.SOURCE) {

    override fun getCharContent(ignoreEncodingErrors: Boolean) =
            "package pack; " +
            "@Deprecated public class Singleton implements java.util.List {" +
            "" +
            "public static Singleton INSTANCE = new Singleton();" +
            "" +
            "private Singleton() {}" +
            "" +
            "private Singleton(String str) {}" +
            "" +
            "private boolean field = true;" +
            "" +
            "private Boolean getBooleanField() { return field; }" +
            "" +
            "private StaticClass getStaticClass() { return new StaticClass(); }" +
            "" +
            "@Override public boolean getField(String args) { return field; }" +
            "" +
            "public UnknownClass getUnknownClass() { return null; }" +
            "" +
            "private static class StaticClass {}" +
            "" +
            "}"

}

private class KotlinFileObject3 : SimpleJavaFileObject(URI("pack/Enum.java"), JavaFileObject.Kind.SOURCE) {

    override fun getCharContent(ignoreEncodingErrors: Boolean) =
            "package pack; " +
            "public enum Enum {" +
            "SINGLE;" +
            "" +
            "}"

}

private class KotlinFileObject4 : SimpleJavaFileObject(URI("pack/SimpleClass.java"), JavaFileObject.Kind.SOURCE) {

    override fun getCharContent(ignoreEncodingErrors: Boolean) =
            "package pack; " +
            "public class SimpleClass {" +
            "private int smth = 1;" +
            "" +
            "private double[] smth2;" +
            "" +
            "}"

}