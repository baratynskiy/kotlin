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

package org.jetbrains.kotlin.treeWrappers

import com.sun.source.util.TreePath
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.Javac
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

abstract class ClassifierType<out T : JCTree>(tree: T,
                                              treePath: TreePath,
                                              javac: Javac) : JCType<T>(tree, treePath, javac), JavaClassifierType {
    override val classifier
        get() = getClassifier(treePath, javac)

    override val canonicalText
        get() = treePath.getFqName(javac).asString()

    override val presentableText
        get() = canonicalText

}

class JCClassifierType<out T : JCTree.JCExpression>(tree: T,
                                                    treePath: TreePath,
                                                    javac: Javac) : ClassifierType<T>(tree, treePath, javac) {

    override val typeArguments: List<JavaType>
        get() = emptyList()

    override val isRaw: Boolean
        get() = isRaw(treePath, javac)

}

class JCClassifierTypeWithTypeArgument<out T : JCTree.JCTypeApply>(tree: T,
                                                                   treePath: TreePath,
                                                                   javac: Javac) : ClassifierType<T>(tree, treePath, javac) {

    override val typeArguments: List<JavaType>
        get() = tree.arguments.map { create(it, treePath, javac) }

    override val isRaw: Boolean
        get() = false

}

private fun isRaw(treePath: TreePath, javac: Javac): Boolean {
    val classifier = getClassifier(treePath, javac)

    return classifier.typeParameters.isNotEmpty()
}

private fun getClassifier(treePath: TreePath, javac: Javac) = treePath.getFqName(javac).let { javac.findClass(it) }
                                                              ?: createStubClassifier(treePath, javac)

private fun createStubClassifier(treePath: TreePath, javac: Javac) = object : JavaClass {
    override val isAbstract: Boolean
        get() = false

    override val isStatic: Boolean
        get() = false

    override val isFinal: Boolean
        get() = false

    override val visibility: Visibility
        get() = Visibilities.PUBLIC

    override val typeParameters: List<JavaTypeParameter>
        get() = emptyList()

    override val fqName: FqName?
        get() = treePath.getFqName(javac)

    override val supertypes: Collection<JavaClassifierType>
        get() = emptyList()

    override val innerClasses: Collection<JavaClass>
        get() = emptyList()

    override val outerClass: JavaClass?
        get() = null

    override val isInterface: Boolean
        get() = false

    override val isAnnotationType: Boolean
        get() = false

    override val isEnum: Boolean
        get() = false

    override val lightClassOriginKind: LightClassOriginKind?
        get() = LightClassOriginKind.SOURCE

    override val methods: Collection<JavaMethod>
        get() = emptyList()

    override val fields: Collection<JavaField>
        get() = emptyList()

    override val constructors: Collection<JavaConstructor>
        get() = emptyList()

    override val name: Name
        get() = SpecialNames.safeIdentifier(treePath.leaf.toString())

    override val annotations
        get() = emptyList<JavaAnnotation>()

    override val isDeprecatedInJavaDoc: Boolean
        get() = false

    override fun findAnnotation(fqName: FqName) = null

}