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
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType

class JCClassifierType<out T : JCTree.JCIdent>(tree: T,
                                       treePath: List<JCTree>) : JCType<T>(tree, treePath), JavaClassifierType {

    override val classifier: JavaClassifier?
        get() = null

    override val typeArguments: List<JavaType>
        get() = emptyList()

    override val isRaw: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val canonicalText: String
        get() = tree.toString()

    override val presentableText: String
        get() = canonicalText
}

class JCClassifierTypeWithTypeArgument<out T : JCTree.JCTypeApply>(tree: T,
                                                                   treePath: List<JCTree>) : JCType<T>(tree, treePath), JavaClassifierType {

    override val classifier: JavaClassifier?
        get() = null

    override val typeArguments: List<JavaType>
        get() = tree.arguments.map { JCType.create(it, treePath) }

    override val isRaw: Boolean
        get() = false

    override val canonicalText: String
        get() = tree.toString()

    override val presentableText: String
        get() = canonicalText
}