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

import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.net.URI
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

class JavacTest : KtUsefulTestCase() {

    fun testCommon() {
        ExtendedJavac.getTrees(listOf(KotlinFileObject(), KotlinFileObject2(), KotlinFileObject3()))
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
            "public class Singleton implements java.util.List {" +
            "" +
            "public static Singleton INSTANCE = new Singleton();" +
            "" +
            "private Singleton() {}" +
            "" +
            "private Singleton(String str) {}" +
            "" +
            "private boolean field = true;" +
            "" +
            "public boolean getField() { return field; }" +
            "" +
            "private interface StaticClass {}" +
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