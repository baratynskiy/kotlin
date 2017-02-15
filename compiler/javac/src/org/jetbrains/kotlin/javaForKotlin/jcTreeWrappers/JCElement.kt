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
import org.jetbrains.kotlin.load.java.structure.JavaElement

abstract class JCElement<out T : JCTree>(val tree: T,
                                         val treePath: List<JCTree>) : JavaElement {

    override fun equals(other: Any?): Boolean {
        if (other !is JCElement<*>) return false

        return tree == other.tree
    }

    override fun hashCode() = tree.hashCode()

    override fun toString() = tree.toString()

}
