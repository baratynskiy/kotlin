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
import com.sun.source.util.TreePath
import com.sun.tools.javac.api.JavacTrees
import com.sun.tools.javac.code.Symtab
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.model.JavacElements
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.List as JavacList
import org.jetbrains.kotlin.elementWrappers.JavacClass
import org.jetbrains.kotlin.elementWrappers.JavacPackage
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.treeWrappers.JCClass
import org.jetbrains.kotlin.treeWrappers.JCPackage
import java.io.File
import javax.lang.model.element.TypeElement
import javax.tools.JavaFileManager
import javax.tools.StandardLocation

class Javac(private val javaFiles: Collection<File>,
            private val classPathRoots: List<File>,
            private val outDir: File?) {

    companion object {
        fun getInstance(project: Project): Javac {
            return ServiceManager.getService(project, Javac::class.java)
        }
    }

    private val context = Context()
    private val javac by lazy { JavaCompiler(context) }

    private val symbols by lazy { Symtab.instance(context) }
    private val trees by lazy { JavacTrees.instance(context) }
    private val elements by lazy { JavacElements.instance(context) }

    private val compilationUnits by lazy {
        JavacFileManager.preRegister(context)
        val fileManager = context[JavaFileManager::class.java] as? JavacFileManager
                          ?: return@lazy emptyList<JCTree.JCCompilationUnit>()

        val classPath = fileManager.getLocation(StandardLocation.PLATFORM_CLASS_PATH)
                .toMutableList()
                .apply {
                    classPathRoots.filter { it.name !in map { it.name } }
                            .let { addAll(it) }
                }

        fileManager.setLocation(StandardLocation.CLASS_PATH, classPath)
        val fileObjects = fileManager.getJavaFileObjectsFromFiles(javaFiles).toList().toJavacList()

        javac.parseFiles(fileObjects)
    }

    private val javaClasses by lazy {
        compilationUnits.flatMap { it.typeDecls.map { type -> Pair(it, type) } }
                .map { JCClass(it.second as JCTree.JCClassDecl, trees.getPath(it.first, it.second), this) }
                .mapTo(arrayListOf<JavaClass>()) { it }
    }

    private val javaPackages by lazy {
        compilationUnits.map { JCPackage(it.packageName.toString(), this) }
                .mapTo(arrayListOf<JavaPackage>()) { it }
    }


    fun findClass(fqName: FqName) = javaClasses.find { it.fqName == fqName } ?: findClassInSymbols(fqName.asString())

    fun findPackage(fqName: FqName) = javaPackages.find { it.fqName == fqName } ?: findPackageInSymbols(fqName.asString())

    fun findSubPackages(fqName: FqName) = symbols.packages
            .filter { (k, _) ->
                k.toString().startsWith(fqName.asString()) && k.toString() != fqName.asString()
            }
            .map { JavacPackage(it.value, this) }
            .toMutableList<JavaPackage>()
            .apply {
                javaPackages.filter { it.fqName.isSubpackageOf(fqName) && it.fqName != fqName }
                        .let { addAll(it) }
            }

    fun findClassesFromPackage(fqName: FqName) = javaClasses
            .filter { it.fqName?.isChildOf(fqName) ?: false }
            .toMutableSet<JavaClass>()
            .also {
                elements.getPackageElement(fqName.asString())
                        ?.enclosedElements
                        ?.filterIsInstance(TypeElement::class.java)
                        ?.map { JavacClass(it, this) }
                        ?.let { classes -> it.addAll(classes) }
            }

    fun getTreePath(tree: JCTree, compilationUnit: CompilationUnitTree): TreePath = trees.getPath(compilationUnit, tree)


    private inline fun <reified T> List<T>.toJavacList() = JavacList.from(toTypedArray())

    private fun findClassInSymbols(fqName: String) = elements.getTypeElement(fqName)
            ?.takeIf {
                // take if it is not a Kotlin binary class from an output directory
                if (outDir == null)
                    true
                else
                    !(it.sourcefile?.name?.endsWith(".kt") ?: false
                      && it.classfile?.toUri()?.path?.startsWith(outDir.toURI().path) ?: false)
            }
            ?.let { JavacClass(it, this) }
            ?.also { javaClasses.add(it) }

    private fun findPackageInSymbols(fqName: String) = elements.getPackageElement(fqName)
            ?.let { JavacPackage(it, this) }
            ?.also { javaPackages.add(it) }

}