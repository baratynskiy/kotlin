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

import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeInfo
import org.jetbrains.kotlin.ExtendedJavac
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType

class JCClassifierType<out T : JCTree.JCIdent>(tree: T,
                                       treePath: List<JCTree>) : JCType<T>(tree, treePath), JavaClassifierType {

    override val classifier: JavaClassifier?
        get() = getClassifier(tree, treePath)

    override val typeArguments: List<JavaType>
        get() = emptyList()

    override val isRaw: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val canonicalText: String
        get() = getFqName(treePath)

    override val presentableText: String
        get() = canonicalText
}

class JCClassifierTypeWithTypeArgument<out T : JCTree.JCTypeApply>(tree: T,
                                                                   treePath: List<JCTree>) : JCType<T>(tree, treePath), JavaClassifierType {

    override val classifier: JavaClassifier?
        get() = getClassifier(tree, treePath)

    override val typeArguments: List<JavaType>
        get() = tree.arguments.map { JCType.create(it, treePath) }

    override val isRaw: Boolean
        get() = false

    override val canonicalText: String
        get() = getFqName(treePath)

    override val presentableText: String
        get() = canonicalText
}

private fun getFqName(treePath: List<JCTree>): String {
    val simpleName = treePath.first().toString().substringBefore("<")
    val compilationUnit = treePath.last() as JCTree.JCCompilationUnit

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
                val type = JCClass(it, TreeInfo.pathFor(it, compilationUnit))
                return type.fqName.asString()
            }

    ExtendedJavac.findClasses(simpleName)
            .filter { it.fullname.toString().startsWith("java.lang.") }
            .firstOrNull()
            ?.let { return it.fullname.toString() }

    return simpleName
}

private fun getClassifier(tree: JCTree, treePath: List<JCTree>): JavaClassifier? {


    return null
}