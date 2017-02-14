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

package org.jetbrains.kotlin.cli.jvm.javac.javaForKotlin.jcTreeWrappers

import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.name.Name

class JCField<out T : JCTree.JCVariableDecl>(tree: T,
                                             parent: JCClass<JCTree.JCClassDecl>) : JCMember<T>(tree, parent), JavaField {

    override val name = Name.identifier(tree.name.toString())

    override val isAbstract = tree.modifiers.isAbstract

    override val isStatic = tree.modifiers.isStatic

    override val isFinal = tree.modifiers.isFinal

    override val visibility = tree.modifiers.visibility

    override val isEnumEntry = tree.modifiers.flags and Flags.ENUM.toLong() != 0L

    override val type: JavaType
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}