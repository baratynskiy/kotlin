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

package org.jetbrains.kotlin

import com.sun.tools.javac.code.Symtab
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeInfo
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.javaForKotlin.jcTreeWrappers.JCClass
import org.jetbrains.kotlin.javaForKotlin.jcTreeWrappers.JCPackage
import org.jetbrains.kotlin.javaForKotlin.wrappers.JavacClass
import org.jetbrains.kotlin.javaForKotlin.wrappers.JavacPackage
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.name.isSubpackageOf
import java.io.File
import javax.lang.model.element.PackageElement
import javax.tools.JavaFileManager
import com.sun.tools.javac.util.List as JavacList
import javax.tools.JavaFileObject

object ExtendedJavac {

    private val context = Context()
    private val javac by lazy { JavaCompiler(context) }

    private val symbols by lazy { Symtab.instance(context) }

    private val javaClasses = arrayListOf<JavaClass>()
    private val javaPackages = hashSetOf<JavaPackage>()

    fun findClasses(simpleName: String) = symbols.classes
            .filter { (k, _) -> k.toString().endsWith(simpleName) }
            .map { it.value }

    fun findSubPackages(pack: PackageElement) = symbols.packages
            .filter { (k, _) -> k.toString().startsWith(pack.qualifiedName.toString()) }
            .map { it.value }

    fun findSubPackages(pack: JavaPackage) = javaPackages.filter { it.fqName.isSubpackageOf(pack.fqName) }

    fun findPackageClasses(pack: JavaPackage) = javaClasses.filter { it.fqName?.isChildOf(pack.fqName) ?: false }

    private fun JavaClass.allInnerClasses(): List<JavaClass> = arrayListOf(this).also {
        innerClasses.forEach { inner -> it.addAll(inner.allInnerClasses()) }
    }

    fun findClass(fqName: String) = javaClasses
            .filter { it.fqName != null && fqName.startsWith(it.fqName!!.asString()) }
            .firstOrNull()
            ?.let {
                if (it.fqName?.asString() == fqName) {
                    it
                } else {
                    it.allInnerClasses().firstOrNull { it.fqName?.asString() == fqName }
                }
            }

    fun findStandardClass(fqName: String) = symbols.classes
            .filter { (k, _) -> k.toString() == fqName }
            .map { it.value }
            .firstOrNull()
            ?.let(::JavacClass)

    fun findPackage(fqName: String) = javaPackages.find { it.fqName.asString() == fqName }

    fun findStandardPackage(fqName: String) = symbols.packages
            .filter { (k, _) -> k.toString() == fqName }
            .map { it.value }
            .firstOrNull()
            ?.let(::JavacPackage)

    fun getTrees(files: Collection<File>) {
        JavacFileManager.preRegister(context)

        val fileManager = context[JavaFileManager::class.java] as? JavacFileManager ?: return
        val fileObjects = files.map { fileManager.getRegularFile(it) }

        getTrees(fileObjects)
    }

    fun getTrees(fileObjects: List<JavaFileObject>) {
        val javacList = fileObjects.toJavacList()

        val compilationUnits = javac.parseFiles(javacList)

        compilationUnits.forEach { compilationUnit ->
            JCPackage(compilationUnit.packageName.toString()).let { javaPackages.add(it) }

            compilationUnit.typeDecls.forEach { type ->
                val treePath = TreeInfo.pathFor(type, compilationUnit)
                JCClass(type as JCTree.JCClassDecl, treePath).let { javaClasses.add(it) }
            }
        }

        javaClasses.first().fields
                .map(JavaField::type)
                .filterIsInstance<JavaClassifierType>()
                .map { "${it.canonicalText}: ${it.isRaw}" }
                .let(::println)
    }

}

private fun List<JavaFileObject>.toJavacList() = JavacList.from(toTypedArray())