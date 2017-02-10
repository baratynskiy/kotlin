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

package org.jetbrains.kotlin.cli.jvm.javac

import com.sun.tools.javac.code.Symtab
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.model.JavacElements
import com.sun.tools.javac.model.JavacTypes
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.List
import org.jetbrains.kotlin.cli.jvm.javac.javaToKotlinElements.JavacClass
import javax.tools.JavaFileObject

object JavaWithKotlinCompiler {

    private val context = Context()
    private val symtab by lazy { Symtab.instance(context) }
    val elements by lazy { JavacElements.instance(context) }
    val types by lazy { JavacTypes.instance(context) }

    fun compile() {
        val javac = JavaCompiler(context)

        val javacList: List<JavaFileObject> = List.from(arrayOf(KotlinFileObject()))

        javac.compile(javacList)

        val symbol = symtab.classes.filter { (_,v)  -> v.fullname.toString() == "pack.SomeClass" }
                .values.firstOrNull() ?: return


        val javacClass = JavacClass(symbol)
        println(javacClass.isInterface)
    }

}
