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

import com.sun.source.util.TreePath
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.Javac
import org.jetbrains.kotlin.load.java.structure.JavaConstructor
import org.jetbrains.kotlin.name.Name

class JCConstructor<out T : JCTree.JCMethodDecl>(tree: T,
                                                 treePath: TreePath,
                                                 javac: Javac) : JCMember<T>(tree, treePath, javac), JavaConstructor {

    override val name
        get() = Name.identifier(tree.name.toString())

    override val isAbstract
        get() = tree.modifiers.isAbstract

    override val isStatic
        get() = tree.modifiers.isStatic

    override val isFinal
        get() = tree.modifiers.isFinal

    override val visibility
        get() = tree.modifiers.visibility

    override val typeParameters
        get() = tree.typeParameters.map { JCTypeParameter(it, TreePath(treePath, it), javac) }

    override val valueParameters
        get() = tree.parameters
                .map { JCValueParameter(it, TreePath(treePath, it), javac) }

}