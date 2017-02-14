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

import com.sun.tools.javac.code.Symtab
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.cli.jvm.javac.javaForKotlin.jcTreeWrappers.JCClass
import org.jetbrains.kotlin.load.java.structure.JavaClass
import com.sun.tools.javac.util.List as JavacList
import javax.tools.JavaFileObject

object ExtendedJavac {

    private val context = Context()

    private val symbols by lazy { Symtab.instance(context) }
    private val javac by lazy { JavaCompiler(context) }

    private val javaClasses = arrayListOf<JavaClass>()

    fun getTrees(fileObjects: List<JavaFileObject>) {
        val javacList = fileObjects.toJavacList()

        val compilationUnits = javac.parseFiles(javacList)

        compilationUnits.forEach { compilationUnit ->
            val packageName = compilationUnit.packageName.toString()
            compilationUnit.typeDecls
                    .filterIsInstance<JCTree.JCClassDecl>()
                    .forEach {
                        javaClasses.add(JCClass(it, null, packageName))
                    }
        }

        println(javaClasses)
    }

}

private fun List<JavaFileObject>.toJavacList() = JavacList.from(toTypedArray())