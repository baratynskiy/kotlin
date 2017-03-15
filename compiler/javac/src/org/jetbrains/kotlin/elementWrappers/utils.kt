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

package org.jetbrains.kotlin.elementWrappers

import com.sun.tools.javac.jvm.ClassReader
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.load.java.JavaVisibilities
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier

val Element.isAbstract
        get() = safeModifiers().contains(Modifier.ABSTRACT)

val Element.isStatic
        get() = safeModifiers().contains(Modifier.STATIC)

val Element.isFinal
        get() = safeModifiers().contains(Modifier.FINAL)

fun Element.getVisibility() = when {
    safeModifiers().contains(Modifier.PUBLIC) -> Visibilities.PUBLIC
    safeModifiers().contains(Modifier.PRIVATE) -> Visibilities.PRIVATE
    safeModifiers().contains(Modifier.PROTECTED) -> {
        if (safeModifiers().contains(Modifier.STATIC)) {
            JavaVisibilities.PROTECTED_STATIC_VISIBILITY
        } else {
            JavaVisibilities.PROTECTED_AND_PACKAGE
        }
    }
    else -> JavaVisibilities.PACKAGE_VISIBILITY
}

// get modifiers in a safe way
private fun Element.safeModifiers(): Set<Modifier> {
    try {
        return modifiers
    } catch (ex: ClassReader.BadClassFile) {
        return emptySet()
    }
}
