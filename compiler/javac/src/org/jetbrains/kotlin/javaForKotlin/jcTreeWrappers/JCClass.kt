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

import com.sun.source.tree.Tree
import com.sun.source.util.TreePath
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.JCTree.JCFieldAccess
import com.sun.tools.javac.tree.TreeInfo
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames

class JCClass<out T : JCTree.JCClassDecl>(tree: T,
                                          treePath: TreePath) : JCClassifier<T>(tree, treePath), JavaClass {

    override val name = SpecialNames.safeIdentifier(tree.simpleName.toString())

    override val annotations: Collection<JavaAnnotation>
        get() = emptyList()

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null

    override val isAbstract = tree.modifiers.isAbstract

    override val isStatic = tree.modifiers.isStatic

    override val isFinal = tree.modifiers.isFinal

    override val visibility = tree.modifiers.visibility

    override val typeParameters
        get() = tree.typeParameters.map { JCTypeParameter(it, TreePath(treePath, it)) }

    override val fqName
        get() = treePath.reversed().joinToString(separator = ".") { (it as? JCTree.JCCompilationUnit)?.packageName?.toString() ?: (it as JCTree.JCClassDecl).name }
                .let(::FqName)

    override val supertypes
        get() = arrayListOf<JavaClassifierType>().apply {
            fun JCTree.mapToJavaClassifierType() = when {
                this is JCTree.JCExpression -> JCClassifierType(this, TreePath(treePath, this))
                this is JCTree.JCTypeApply -> JCClassifierTypeWithTypeArgument(this, TreePath(treePath, this))
                else -> null
            }

            tree.extending?.mapToJavaClassifierType()?.let { add(it) }
            tree.implementing?.map { it.mapToJavaClassifierType() }?.filterNotNull()?.let { addAll(it) }
        }

    override val innerClasses
        get() = tree.members
                .filterIsInstance(JCTree.JCClassDecl::class.java)
                .map { JCClass(it, TreePath(treePath, it)) }

    override val outerClass
        get() = (treePath.firstOrNull() as? JCTree.JCClassDecl)?.let { JCClass<JCTree.JCClassDecl>(it, treePath.parentPath) }

    override val isInterface = tree.modifiers.flags and Flags.INTERFACE.toLong() != 0L

    override val isAnnotationType = tree.modifiers.flags and Flags.ANNOTATION.toLong() != 0L

    override val isEnum = tree.modifiers.flags and Flags.ENUM.toLong() != 0L

    override val lightClassOriginKind = null

    override val methods
        get() = tree.members
                .filterIsInstance(JCTree.JCMethodDecl::class.java)
                .filter { it.kind == Tree.Kind.METHOD }
                .filter { it.name.toString() != "<init>" }
                .map { JCMethod(it, TreePath(treePath, it)) }

    override val fields
        get() = tree.members
                .filterIsInstance(JCTree.JCVariableDecl::class.java)
                .map { JCField(it, TreePath(treePath, it)) }

    override val constructors
        get() = tree.members
                .filterIsInstance(JCTree.JCMethodDecl::class.java)
                .filter { TreeInfo.isConstructor(it) }
                .map { JCConstructor(it, TreePath(treePath, it)) }
}