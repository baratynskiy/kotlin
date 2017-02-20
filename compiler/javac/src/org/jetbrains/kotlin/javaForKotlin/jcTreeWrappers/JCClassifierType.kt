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

package org.jetbrains.kotlin.javaForKotlin.jcTreeWrappers

import com.sun.source.util.TreePath
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.ExtendedJavac
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType

abstract class ClassifierType<out T : JCTree>(tree: T,
                                              treePath: TreePath) : JCType<T>(tree, treePath), JavaClassifierType {
    override val classifier: JavaClassifier?
        get() = getClassifier(treePath)

    override val canonicalText: String
        get() = getFqName(treePath)

    override val presentableText: String
        get() = canonicalText

}

class JCClassifierType<out T : JCTree.JCExpression>(tree: T,
                                       treePath: TreePath) : ClassifierType<T>(tree, treePath) {

    override val typeArguments: List<JavaType>
        get() = emptyList()

    override val isRaw: Boolean
        get() = isRaw(treePath)

}

class JCClassifierTypeWithTypeArgument<out T : JCTree.JCTypeApply>(tree: T,
                                                                   treePath: TreePath) : ClassifierType<T>(tree, treePath) {

    override val typeArguments: List<JavaType>
        get() = tree.arguments.map { JCType.create(it, treePath) }

    override val isRaw: Boolean
        get() = false

}

private fun isRaw(treePath: TreePath): Boolean {
    val classifier = getClassifier(treePath) as? JavaClass ?: return false

    return classifier.typeParameters.isNotEmpty()
}

private fun getFqName(treePath: TreePath): String {
    val simpleName = treePath.leaf.toString().substringBefore("<")
    val compilationUnit = treePath.compilationUnit as JCTree.JCCompilationUnit

    val importStatement = compilationUnit.imports.firstOrNull { it.qualifiedIdentifier.toString().endsWith(".$simpleName") }
    importStatement?.let { return it.qualifiedIdentifier.toString() }

    fun JCTree.JCClassDecl.innerClasses(): List<JCTree.JCClassDecl> = arrayListOf(this).also {
        it.addAll(members.filterIsInstance<JCTree.JCClassDecl>()
                          .flatMap(JCTree.JCClassDecl::innerClasses))
    }

    compilationUnit.typeDecls
            .filterIsInstance<JCTree.JCClassDecl>()
            .flatMap(JCTree.JCClassDecl::innerClasses)
            .filter {
                it.simpleName.toString() == simpleName
            }
            .firstOrNull()
            ?.let {
                val type = JCClass(it, ExtendedJavac.getTreePath(it, compilationUnit))
                return type.fqName.asString()
            }

    ExtendedJavac.findClasses(simpleName)
            .filter { it.fullname.toString().startsWith("java.lang.") }
            .firstOrNull()
            ?.let { return it.fullname.toString() }

    return simpleName
}

private fun getClassifier(treePath: TreePath): JavaClassifier? {
    val fqName = getFqName(treePath)

    ExtendedJavac.findClass(fqName)?.let { return it }
    ExtendedJavac.findStandardClass(fqName)?.let { return it }

    return null
}