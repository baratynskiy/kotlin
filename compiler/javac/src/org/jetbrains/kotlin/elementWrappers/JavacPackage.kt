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

import org.jetbrains.kotlin.Javac
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

class JavacPackage(val element: PackageElement, val javac: Javac) : JavaPackage {

    override val fqName
        get() = element.qualifiedName.let { FqName(it.toString()) }

    override val subPackages
        get() = javac.findSubPackages(element).map { JavacPackage(it, javac) }

    override fun getClasses(nameFilter: (Name) -> Boolean) = element.enclosedElements
            .filterIsInstance(TypeElement::class.java)
            .filter { Name.isValidIdentifier(it.simpleName.toString())
                      && nameFilter(Name.identifier(it.simpleName.toString()))
            }
            .map { JavacClass(it, javac) }

    override fun hashCode() = element.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other !is JavacPackage) return false

        return element == other.element
    }

    override fun toString() = element.qualifiedName.toString()

}