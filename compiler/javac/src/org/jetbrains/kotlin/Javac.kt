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

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.sun.source.tree.CompilationUnitTree
import com.sun.tools.javac.api.JavacTrees
import com.sun.tools.javac.code.Symtab
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.model.JavacElements
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Names
import com.sun.tools.javac.util.Options
import org.jetbrains.kotlin.treeWrappers.JCClass
import org.jetbrains.kotlin.treeWrappers.JCPackage
import org.jetbrains.kotlin.elementWrappers.JavacClass
import org.jetbrains.kotlin.elementWrappers.JavacPackage
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.name.isSubpackageOf
import java.io.File
import javax.lang.model.element.PackageElement
import javax.tools.JavaFileManager
import javax.tools.StandardLocation
import com.sun.tools.javac.util.List as JavacList

class Javac(private val javaFiles: Collection<File>,
            private val classPathRoots: List<File>) {

    private val context = Context()
    private val javac by lazy { JavaCompiler(context) }
    private val options by lazy { Options.instance(context).apply { put("-cp", classPathRoots.joinToString(separator = ";") { it.absolutePath }) } }

    private val names by lazy { Names.instance(context) }
    private val symbols by lazy { Symtab.instance(context) }
    private val trees by lazy { JavacTrees.instance(context) }
    private val elements by lazy { JavacElements.instance(context) }

    private val compilationUnits by lazy {
        JavacFileManager.preRegister(context)

        val fileManager = context[JavaFileManager::class.java] as? JavacFileManager
                          ?: return@lazy emptyList<JCTree.JCCompilationUnit>()
        fileManager.setLocation(StandardLocation.CLASS_PATH, classPathRoots)

        val fileObjects = fileManager.getJavaFileObjectsFromFiles(javaFiles).toList().toJavacList()

        javac.parseFiles(fileObjects)
    }

    private val javaClasses: ArrayList<JavaClass> by lazy {
        compilationUnits.flatMap { it.typeDecls.map { type -> Pair(it, type) } }
                .map { JCClass(it.second as JCTree.JCClassDecl, trees.getPath(it.first, it.second), this) }
                .mapTo(arrayListOf<JavaClass>()) { it }
    }

    private val javaPackages: ArrayList<JavaPackage> by lazy {
        compilationUnits.map { JCPackage(it.packageName.toString(), this) }
                .mapTo(arrayListOf<JavaPackage>()) { it }
    }

    fun findClasses(simpleName: String) = symbols.classes
            .filter { (k, _) -> k.toString().endsWith(simpleName) }
            .map { it.value }

    fun findSubPackages(pack: PackageElement) = symbols.packages
            .filter { (k, _) -> k.toString().startsWith(pack.qualifiedName.toString()) }
            .map { it.value }

    fun findSubPackages(pack: JavaPackage) = javaPackages.filter { it.fqName.isSubpackageOf(pack.fqName) }

    fun findPackageClasses(pack: JavaPackage) = javaClasses.filter { it.fqName!!.isChildOf(pack.fqName) }

    fun getTreePath(tree: JCTree, compilationUnit: CompilationUnitTree) = trees.getPath(compilationUnit, tree)

    fun findClass(fqName: String) = javaClasses
                   .filter { fqName.startsWith(it.fqName!!.asString()) }
                   .firstOrNull()
                   ?.let {
                       if (it.fqName!!.asString() == fqName) {
                           it
                       } else {
                           it.allInnerClasses().firstOrNull { it.fqName?.asString() == fqName }
                       }
                   } ?: findClassInSymbols(fqName)

    fun findPackage(fqName: String) = javaPackages.find { it.fqName.asString() == fqName } ?: findPackageInSymbols(fqName)

    private inline fun <reified T> List<T>.toJavacList() = JavacList.from(toTypedArray())

    private fun JavaClass.allInnerClasses(): List<JavaClass> = arrayListOf(this).also {
        innerClasses.forEach { inner -> it.addAll(inner.allInnerClasses()) }
    }

    private fun findClassInSymbols(fqName: String) = elements.getTypeElement(fqName)
            ?.let { JavacClass(it, this) }
            ?.also { javaClasses.add(it) }

    private fun findPackageInSymbols(fqName: String) = elements.getPackageElement(fqName)
            ?.let { JavacPackage(it, this) }
            ?.also { javaPackages.add(it) }

    companion object {
        fun getInstance(project: Project): Javac {
            return ServiceManager.getService(project, Javac::class.java)
        }
    }

}