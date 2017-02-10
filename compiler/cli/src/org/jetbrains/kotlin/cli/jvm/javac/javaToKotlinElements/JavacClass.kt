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

import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind

class JavacClass<T : TypeElement>(element: T) : JavacClassifier<TypeElement>(element), JavaClass {

    override val name = SpecialNames.safeIdentifier(element.simpleName.toString())

    override val isAbstract = element.isAbstract

    override val isStatic = element.isStatic

    override val isFinal = element.isFinal

    override val visibility = element.getVisibility()

    override val typeParameters: List<JavaTypeParameter>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val fqName = FqName(element.qualifiedName.toString())

    override val supertypes: Collection<JavaClassifierType>
            get() = TODO()

    override val innerClasses: Collection<JavaClass>
        get() = element.enclosedElements
                .filter{ it.asType().kind == TypeKind.DECLARED }
                .filterIsInstance(TypeElement::class.java)
                .map { JavacClass(it) }

    override val outerClass: JavaClass?
        get() = element.enclosingElement?.let {
            if (it.asType().kind != TypeKind.DECLARED) null else JavacClass(it as TypeElement)
        }

    override val isInterface = element.kind == ElementKind.INTERFACE

    override val isAnnotationType = element.kind == ElementKind.ANNOTATION_TYPE

    override val isEnum = element.kind == ElementKind.ENUM

    override val lightClassOriginKind = null

    override val methods: Collection<JavaMethod>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val fields: Collection<JavaField>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val constructors: Collection<JavaConstructor>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

}
