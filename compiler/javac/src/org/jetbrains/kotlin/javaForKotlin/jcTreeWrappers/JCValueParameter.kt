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
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class JCValueParameter<out T : JCTree.JCVariableDecl>(tree: T,
                                                      treePath: TreePath) : JCElement<T>(tree, treePath), JavaValueParameter {

    override val annotations: Collection<JavaAnnotation>
        get() = emptyList()

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null

    override val isDeprecatedInJavaDoc = false

    override val name = Name.identifier(tree.name.toString())

    override val type: JavaType
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val isVararg: Boolean
        get() = tree.modifiers.flags and Flags.VARARGS != 0L
}