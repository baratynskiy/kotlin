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
import com.sun.tools.javac.jvm.ClassReader
import com.sun.tools.javac.main.JavaCompiler
import com.sun.tools.javac.model.JavacElements
import com.sun.tools.javac.model.JavacTypes
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.List
import com.sun.tools.javac.util.Name
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.JavaFileObject

object JavaWithKotlinCompiler {

    private val context = Context()
    private val symtab by lazy { Symtab.instance(context) }
    private val classReader by lazy { ClassReader.instance(context) }
    private val names by lazy { Name.Table.instance(context) }

    val elements: Elements by lazy { JavacElements.instance(context) }
    val types: Types by lazy { JavacTypes.instance(context) }

    fun findType(name: String): TypeElement? = symtab.classes
            .filter { (_,v)  -> v.fullname.toString() == name }
            .values.firstOrNull()

    fun findPackage(name: String): PackageElement? = symtab.packages
            .filter { (_,v)  -> v.fullname.toString() == name }
            .values.firstOrNull()

    fun getSubpackages(name: String) = symtab.packages
            .filter { (_,v)  -> v.fullname.toString().startsWith(name) }
            .values

    fun compile() {
        val javac = JavaCompiler(context)
        val javacList: List<JavaFileObject> = List.from(arrayOf(KotlinFileObject()))

        val kotlinPackageName = Name.fromString(names, "pack2")
        val pack = KotlinPackageSymbol(kotlinPackageName, symtab.rootPackage, names, symtab, classReader)

        symtab.packages.put(kotlinPackageName, pack)

        javac.compile(javacList)

        javac.close()
    }

}
