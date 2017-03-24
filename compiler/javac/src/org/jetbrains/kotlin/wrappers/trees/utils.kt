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

package org.jetbrains.kotlin.wrappers.trees

import com.sun.source.tree.AnnotationTree
import com.sun.source.util.TreePath
import com.sun.source.util.TreePathScanner
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.Javac
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import javax.lang.model.element.Modifier

val JCTree.JCModifiers.isAbstract
    get() = Modifier.ABSTRACT in getFlags()

val JCTree.JCModifiers.isFinal
    get() = Modifier.FINAL in getFlags()

val JCTree.JCModifiers.isStatic
    get() = Modifier.STATIC in getFlags()

val JCTree.JCModifiers.visibility
    get() = getFlags().let {
        when {
            Modifier.PUBLIC in it -> Visibilities.PUBLIC
            Modifier.PRIVATE in it -> Visibilities.PRIVATE
            Modifier.PROTECTED in it -> {
                if (Modifier.STATIC in it) JavaVisibilities.PROTECTED_STATIC_VISIBILITY else JavaVisibilities.PROTECTED_AND_PACKAGE
            }
            else -> JavaVisibilities.PACKAGE_VISIBILITY
        }
    }

val TreePath.annotations
        get() = AnnotationSearcher(this).get()

fun TreePath.resolve(javac: Javac): Pair<FqName, JavaClass?> {
    val simpleName = leaf.toString().substringBefore("<").substringAfter("@")
    val compilationUnit = compilationUnit as JCTree.JCCompilationUnit

    val importStatement = compilationUnit.imports.firstOrNull { it.qualifiedIdentifier.toString().endsWith(".$simpleName") }
    importStatement?.let {
        val fqName = FqName(it.qualifiedIdentifier.toString())
        return fqName to javac.findClass(fqName)
    }

    if (simpleName.contains(".")) {
        val packageName = compilationUnit.packageName.toString()
        val fqName = "$packageName.$simpleName"

        return javac.findClass(FqName(fqName))?.let { it.fqName!! to it}
               ?: with(FqName(simpleName)) { this to javac.findClass(this) }
    }

    javac.findClass(FqName("java.lang.$simpleName"))
            ?.let { return it.fqName!! to it }

    val outerClass = parentPath.find { it is JCTree.JCClassDecl } as? JCTree.JCClassDecl
    return javac.findClassesFromPackage(FqName("${compilationUnit.packageName}"))
            .filter { it.name.asString() == simpleName }
            .filter {
                it.outerClass?.let {
                    outerClass?.let { outer -> outer.simpleName.toString() == it.name.asString() } ?: false
                } ?: true
            }
            .firstOrNull()
            ?.let { it.fqName!! to it } ?: FqName("${compilationUnit.packageName}.$simpleName") to null
}

fun JavaClass.computeClassId(): ClassId? {
    val outer = outerClass
    outer?.let {
        val parentClassId = outer.computeClassId() ?: return null
        return parentClassId.createNestedClassId(name)
    }

    return fqName?.let { ClassId.topLevel(it) }
}

class AnnotationSearcher(private val treePath: TreePath) : TreePathScanner<Unit, Unit>() {

    private val annotations = arrayListOf<JCTree.JCAnnotation>()

    fun get() = scan(treePath, Unit).let { annotations }

    override fun visitAnnotation(node: AnnotationTree?, p: Unit?) {
        val owner = currentPath.parentPath.parentPath.leaf
        if (node != null && owner == treePath.leaf) annotations.add(node as JCTree.JCAnnotation)

        super.visitAnnotation(node, p)
    }

}