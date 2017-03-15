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
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType

abstract class ClassifierType<out T : JCTree>(tree: T,
                                              treePath: TreePath,
                                              javac: Javac) : JCType<T>(tree, treePath, javac), JavaClassifierType {
    override val classifier
        get() = getClassifier(treePath, javac)

    override val canonicalText
        get() = treePath.getFqName(javac)

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
    val classifier = getClassifier(treePath, javac) as? JavaClass ?: return false

    return classifier.typeParameters.isNotEmpty()
}

private fun getClassifier(treePath: TreePath, javac: Javac) =  treePath.getFqName(javac).let { javac.findClass(it) }