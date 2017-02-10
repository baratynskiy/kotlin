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

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import javax.lang.model.type.TypeMirror

class JavacPrimitiveType<out T : TypeMirror>(typeMirror: T) : JavacType<T>(typeMirror), JavaPrimitiveType {

    override val type: PrimitiveType?
        get() = if ("void" == typeMirror.toString()) null else JvmPrimitiveType.get(typeMirror.toString()).primitiveType

}
