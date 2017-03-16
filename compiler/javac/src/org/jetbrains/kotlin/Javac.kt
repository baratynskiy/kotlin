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
import com.intellij.psi.search.GlobalSearchScope
import com.sun.source.tree.CompilationUnitTree
import com.sun.source.util.TreePath
import com.sun.tools.javac.api.JavacTrees
import com.sun.tools.javac.code.Symtab
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.model.JavacElements
import com.sun.tools.javac.tree.EndPosTable
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Log
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
import java.io.Closeable
import java.io.File
import javax.lang.model.element.TypeElement
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

class Javac(private val javaFiles: Collection<File>,
            classPathRoots: List<File>,
            outDir: File?) : Closeable {

    companion object {
        fun getInstance(project: Project): Javac = ServiceManager.getService(project, Javac::class.java)
    }

    private val context = Context().apply {
        put(Log.logKey, object : Log(this) {
            override fun setEndPosTable(name: JavaFileObject?, table: EndPosTable?) {
                val source = getSource(name)
                if (source.endPosTable == null) {
                    source.endPosTable = table
                }
            }
        })
    }
    private val javac = JavaCompiler(context)
    private val fileManager = context[JavaFileManager::class.java] as JavacFileManager

    init {
        fileManager.setLocation(StandardLocation.CLASS_PATH, classPathRoots)
        outDir?.let {
            it.mkdirs()
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, listOf(it))
        }
    }

    private val symbols by lazy { Symtab.instance(context) }
    private val trees by lazy { JavacTrees.instance(context) }
    private val elements by lazy { JavacElements.instance(context) }
    private val fileObjects by lazy { fileManager.getJavaFileObjectsFromFiles(javaFiles).toList().toJavacList() }
    private val compilationUnits by lazy { javac.parseFiles(fileObjects) }

    private val javaClasses by lazy {
        compilationUnits.flatMap { it.typeDecls.map { type -> Pair(it, type) } }
                .map { JCClass(it.second as JCTree.JCClassDecl, trees.getPath(it.first, it.second), this) }
                .mapTo(arrayListOf<JavaClass>()) { it }
    }

    private val javaPackages by lazy {
        compilationUnits.map { JCPackage(it.packageName.toString(), this) }
                .mapTo(arrayListOf<JavaPackage>()) { it }
    }

    fun compile() {
        val cp = fileManager.getLocation(StandardLocation.CLASS_PATH) + fileManager.getLocation(StandardLocation.CLASS_OUTPUT)
        fileManager.setLocation(StandardLocation.CLASS_PATH, cp)

        val newContext = Context()
        newContext.put(JavaFileManager::class.java, fileManager)

        val newFileManager = newContext[JavaFileManager::class.java] as JavacFileManager

        newFileManager.setLocation(StandardLocation.CLASS_PATH, cp)
        newFileManager.setLocation(StandardLocation.CLASS_OUTPUT, fileManager.getLocation(StandardLocation.CLASS_OUTPUT))

        JavaCompiler(newContext).compile(fileObjects)
    }

    override fun close() {
        fileManager.close()
        javac.close()
    }

    fun findClass(fqName: FqName, scope: GlobalSearchScope? = null) = scope?.let {
        if ("$it".startsWith("NOT:"))
            findClassInSymbols(fqName.asString())
        else
            javaClasses.find { it.fqName == fqName }
    } ?: findClass(fqName)

    fun findPackage(fqName: FqName, scope: GlobalSearchScope) = scope.let {
        if ("$it".startsWith("NOT:"))
            findPackageInSymbols(fqName.asString())
        else
            javaPackages.find { it.fqName == fqName }
    }

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
            .toMutableSet()
            .also {
                elements.getPackageElement(fqName.asString())
                        ?.members()
                        ?.elements
                        ?.filterIsInstance(TypeElement::class.java)
                        ?.map { JavacClass(it, this) }
                        ?.let { classes -> it.addAll(classes) }
            }

    fun getTreePath(tree: JCTree, compilationUnit: CompilationUnitTree): TreePath = trees.getPath(compilationUnit, tree)

    private inline fun <reified T> List<T>.toJavacList() = JavacList.from(toTypedArray())

    private fun findClass(fqName: FqName) = javaClasses.find { it.fqName == fqName } ?: findClassInSymbols(fqName.asString())

    private fun findClassInSymbols(fqName: String) = elements.getTypeElement(fqName)?.let { JavacClass(it, this) }

    private fun findPackageInSymbols(fqName: String) = elements.getPackageElement(fqName)
            ?.let { JavacPackage(it, this) }
}