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

package org.jetbrains.kotlin.cli.jvm.javac.javaToKotlinElements

import org.jetbrains.kotlin.cli.jvm.javac.JavaWithKotlinCompiler
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import javax.lang.model.element.VariableElement

class JavacValueParameter<out T : VariableElement>(element: T, name : String,
                                                   override val isVararg : Boolean) : JavacElement<T>(element), JavaValueParameter {

    override val annotations: Collection<JavaAnnotation>
        get() = element.annotationMirrors
                .map(::JavacAnnotation)

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = element.annotationMirrors
            .filter { it.toString() == fqName.asString() }
            .firstOrNull()
            ?.let(::JavacAnnotation)

    override val isDeprecatedInJavaDoc = JavaWithKotlinCompiler.elements.isDeprecated(element)

    override val name = Name.identifier(name)

    override val type: JavaType
        get() = JavacType.create(element.asType())

}