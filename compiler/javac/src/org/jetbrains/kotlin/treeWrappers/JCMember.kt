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
import org.jetbrains.kotlin.load.java.structure.JavaMember
import org.jetbrains.kotlin.name.FqName

abstract class JCMember<out T : JCTree>(tree: T,
                                        treePath: TreePath,
                                        javac: Javac) : JCElement<T>(tree, treePath, javac), JavaMember {

    override val containingClass
        get() = (treePath.parentPath.leaf as JCTree.JCClassDecl).let { JCClass(it, TreePath(treePath, it), javac) }

    override val isDeprecatedInJavaDoc = false

    override val annotations
        get() = treePath.annotations.map { JCAnnotation(it, TreePath.getPath(treePath.compilationUnit, it), javac) }

    override fun findAnnotation(fqName: FqName) = annotations.firstOrNull { it.classId?.asSingleFqName() == fqName }

}
