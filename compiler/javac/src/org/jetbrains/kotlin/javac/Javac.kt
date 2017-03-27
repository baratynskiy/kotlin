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

package org.jetbrains.kotlin.javac

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.CommonClassNames
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import com.sun.source.tree.CompilationUnitTree
import com.sun.source.util.TreePath
import com.sun.tools.javac.api.JavacTrees
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Symtab
import com.sun.tools.javac.file.JavacFileManager
import com.sun.tools.javac.jvm.ClassReader
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.model.JavacElements
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Names
import com.sun.tools.javac.util.List as JavacList
import org.jetbrains.kotlin.wrappers.symbols.JavacClass
import org.jetbrains.kotlin.wrappers.symbols.JavacPackage
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.wrappers.trees.JCClass
import org.jetbrains.kotlin.wrappers.trees.JCPackage
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

    val JAVA_LANG_OBJECT by lazy { findClassInSymbols(CommonClassNames.JAVA_LANG_OBJECT) }

    private val context = Context()
    private val javac = object : JavaCompiler(context) {
        override fun parseFiles(files: Iterable<JavaFileObject>?) = compilationUnits
    }
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
    private val fileObjects by lazy { fileManager.getJavaFileObjectsFromFiles(javaFiles).toJavacList() }
    private val compilationUnits: JavacList<JCTree.JCCompilationUnit> by lazy { fileObjects.map(javac::parse).toJavacList() }

    private val javaClasses by lazy {
        compilationUnits
                .flatMap { unit -> unit.typeDecls.map { unit to it } }
                .map { JCClass(it.second as JCTree.JCClassDecl, trees.getPath(it.first, it.second), this) }
                .flatMap { it.withInnerClasses() }
    }

    private val javaPackages by lazy {
        compilationUnits.map { JCPackage(it.packageName.toString(), this) }
    }

    fun compile() = fileManager.setClassPathBeforeCompilation().let {
        javac.compile(fileObjects)
        javac.errorCount() == 0
    }

    override fun close() {
        fileManager.close()
        javac.close()
    }

    fun findClass(fqName: FqName, scope: GlobalSearchScope = EverythingGlobalScope()) = when {
        scope is EverythingGlobalScope -> findClass(fqName)
        scope.contains(AnyJavaSourceVirtualFile) -> javaClasses.find { it.fqName == fqName }
        else -> findClassInSymbols(fqName.asString()) ?: javaClasses.find { it.fqName == fqName }
    }

    fun findPackage(fqName: FqName, scope: GlobalSearchScope) = when {
        scope is EverythingGlobalScope -> findPackage(fqName)
        scope.contains(AnyJavaSourceVirtualFile) -> javaPackages.find { it.fqName == fqName }
        else -> findPackageInSymbols(fqName.asString()) ?: javaPackages.find { it.fqName == fqName }
    }

    fun findSubPackages(fqName: FqName) = symbols.packages
                                                  .filter { (k, _) -> k.toString().startsWith("$fqName.") }
                                                  .map { JavacPackage(it.value, this) } + javaPackages
                                                  .filter { it.fqName.isSubpackageOf(fqName) && it.fqName != fqName }

    fun findClassesFromPackage(fqName: FqName) = javaClasses.filter { it.fqName?.parentOrNull() == fqName }
                                                         .flatMap { it.withInnerClasses() } +
                                                 elements.getPackageElement(fqName.asString())
                                                         ?.members()
                                                         ?.elements
                                                         ?.filterIsInstance(TypeElement::class.java)
                                                         ?.map { JavacClass(it, this) }
                                                         .orEmpty()

    fun getTreePath(tree: JCTree, compilationUnit: CompilationUnitTree): TreePath = trees.getPath(compilationUnit, tree)

    private inline fun <reified T> Iterable<T>.toJavacList() = JavacList.from(this)

    private fun findClass(fqName: FqName) = javaClasses.find { it.fqName == fqName } ?: findClassInSymbols(fqName.asString())

    private fun findPackage(fqName: FqName) = javaPackages.find { it.fqName == fqName } ?: findPackageInSymbols(fqName.asString())

    private fun findClassInSymbols(fqName: String) = elements.getTypeElement(fqName)?.let { JavacClass(it, this) }

    private fun findPackageInSymbols(fqName: String) = elements.getPackageElement(fqName)?.let { JavacPackage(it, this) }

    private fun JavacFileManager.setClassPathBeforeCompilation() = apply {
        setLocation(StandardLocation.CLASS_PATH,
                    getLocation(StandardLocation.CLASS_PATH) + getLocation(StandardLocation.CLASS_OUTPUT))

        val reader = ClassReader.instance(context)
        val names = Names.instance(context)
        val outDirName = getLocation(StandardLocation.CLASS_OUTPUT).firstOrNull()?.path ?: ""

        list(StandardLocation.CLASS_OUTPUT, "", setOf(JavaFileObject.Kind.CLASS), true)
                .forEach {
                    val fqName = it.name
                            .substringAfter(outDirName)
                            .substringBefore(".class")
                            .replace("/", ".")
                            .let { if (it.startsWith(".")) it.substring(1) else it }
                            .let(names::fromString)

                    symbols.classes[fqName]?.let { symbols.classes[fqName] = null }
                    val symbol = reader.enterClass(fqName, it)

                    (elements.getPackageOf(symbol) as? Symbol.PackageSymbol)?.let {
                        it.members_field.enter(symbol)
                        it.flags_field = it.flags_field or Flags.EXISTS.toLong()
                    }
                }

    }

    private fun JavaClass.withInnerClasses(): List<JavaClass> = listOf(this) + innerClasses.flatMap { it.withInnerClasses() }

}

private object AnyJavaSourceVirtualFile : VirtualFile() {
    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}

    override fun getLength() = 0L

    override fun getFileSystem() = throw UnsupportedOperationException()

    override fun getPath() = ""

    override fun isDirectory() = false

    override fun getTimeStamp() = 0L

    override fun getName() = ""

    override fun contentsToByteArray() = throw UnsupportedOperationException()

    override fun isValid() = true

    override fun getInputStream() = throw UnsupportedOperationException()

    override fun getParent() = throw UnsupportedOperationException()

    override fun getChildren(): Array<VirtualFile> = emptyArray()

    override fun isWritable() = false

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long) = throw UnsupportedOperationException()

    override fun getExtension() = "java"

    override fun getFileType(): FileType = JavaFileType.INSTANCE

    override fun toString() = "Java Source"
}