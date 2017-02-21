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

import com.sun.source.tree.CompilationUnitTree
import com.sun.source.util.TreePath
import com.sun.tools.javac.api.JavacTrees
import com.sun.tools.javac.code.Symtab
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
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
import com.sun.tools.javac.util.List as JavacList
import javax.tools.JavaFileObject

class Javac(private val javaFiles: Collection<File>) {

    private val context = Context()
    private val javac by lazy { JavaCompiler(context) }

    private val symbols by lazy { Symtab.instance(context) }
    private val trees by lazy { JavacTrees.instance(context) }

    private val compilationUnits by lazy {
        JavacFileManager.preRegister(context)

        val fileManager = context[JavaFileManager::class.java] as? JavacFileManager
                          ?: return@lazy emptyList<JCTree.JCCompilationUnit>()
        val fileObjects = javaFiles.map { fileManager.getRegularFile(it) }.toJavacList()

        javac.parseFiles(fileObjects)
    }

    private val javaClasses by lazy {
        compilationUnits.flatMap { it.typeDecls.map { typeDecl -> Pair(it, typeDecl) } }
                .map { JCClass(it.second as JCTree.JCClassDecl, trees.getPath(it.first, it.second), this) }
    }

    private val javaPackages by lazy {
        compilationUnits.map { JCPackage(it.packageName.toString(), this) }
    }

    fun findClasses(simpleName: String) = symbols.classes
            .filter { (k, _) -> k.toString().endsWith(simpleName) }
            .map { it.value }

    fun findSubPackages(pack: PackageElement) = symbols.packages
            .filter { (k, _) -> k.toString().startsWith(pack.qualifiedName.toString()) }
            .map { it.value }

    fun findSubPackages(pack: JavaPackage) = javaPackages.filter { it.fqName.isSubpackageOf(pack.fqName) }

    fun findPackageClasses(pack: JavaPackage) = javaClasses.filter { it.fqName.isChildOf(pack.fqName) }

    fun getTreePath(tree: JCTree, compilationUnit: CompilationUnitTree): TreePath = trees.getPath(compilationUnit, tree)

    fun findClass(fqName: String): JavaClass? {
        val vl =  javaClasses
                       .filter { fqName.startsWith(it.fqName.asString()) }
                       .firstOrNull()
                       ?.let {
                           if (it.fqName.asString() == fqName) {
                               it
                           } else {
                               it.allInnerClasses().firstOrNull { it.fqName?.asString() == fqName }
                           }
                       } ?: findClassInSymbols(fqName)
        return vl
    }

    fun findPackage(fqName: String): JavaPackage? {
        val vl = javaPackages.find { it.fqName.asString() == fqName } ?: findPackageInSymbols(fqName)
        return vl
    }

    private fun List<JavaFileObject>.toJavacList() = JavacList.from(toTypedArray())

    private fun JavaClass.allInnerClasses(): List<JavaClass> = arrayListOf(this).also {
        innerClasses.forEach { inner -> it.addAll(inner.allInnerClasses()) }
    }

    private fun findClassInSymbols(fqName: String) = symbols.classes
            .filter { (k, _) -> k.toString() == fqName }
            .map { it.value }
            .firstOrNull()
            ?.let { JavacClass(it, this) }

    private fun findPackageInSymbols(fqName: String) = symbols.packages
            .filter { (k, _) -> k.toString() == fqName }
            .map { it.value }
            .firstOrNull()
            ?.let { JavacPackage(it, this) }

}