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
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaArrayAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

open class JavacAnnotationArgument(fqName: FqName,
                                   val javac: Javac) : JavaAnnotationArgument, JavaElement {

    companion object {
        fun create(value: Any, name: Name, javac: Javac): JavaAnnotationArgument = when (value) {
            is AnnotationMirror -> JavacAnnotationAsAnnotationArgument(value, name, javac)
            is VariableElement -> JavacReferenceAnnotationArgument(value, javac)
            is String -> JavacLiteralAnnotationArgument(value, name, javac)
            is Class<*> -> JavacClassObjectAnnotationArgument(value, name, javac)
            is Collection<*> -> arrayAnnotationArguments(value, name, javac)
            is AnnotationValue -> create(value.value, name, javac)
            is TypeMirror -> JavacLiteralAnnotationArgument(value.toString(), name, javac)
            else -> JavacLiteralAnnotationArgument(value, name, javac)
        }

        private fun arrayAnnotationArguments(values: Collection<*>, name: Name, javac: Javac): JavaArrayAnnotationArgument = values
                .map { if (it is Collection<*>) arrayAnnotationArguments(it, name, javac) else create(it!!, name, javac) }
                .let { JavacArrayAnnotationArgument(it, name, javac) }

    }

    override val name = Name.identifier(fqName.shortName().asString())

}