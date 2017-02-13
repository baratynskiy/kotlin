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

import com.sun.tools.javac.code.Scope
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Symtab
import com.sun.tools.javac.code.Type
import com.sun.tools.javac.util.List
import com.sun.tools.javac.util.Name
import java.net.URI
import javax.lang.model.element.ElementKind
import javax.lang.model.type.TypeKind
import javax.tools.*

// for tests

internal class KotlinFileObject : SimpleJavaFileObject(URI("pack/SomeClass.java"), JavaFileObject.Kind.SOURCE) {

    override fun getCharContent(ignoreEncodingErrors: Boolean) =
            "package pack; import pack2.SomeClass2; public class SomeClass { public static void func() { System.out.println(); } " +
            "/*public static SomeClass2 func2() { return new SomeClass2(); }*/ }"
}

internal class KotlinClassSymbol(name: Name, pack: Symbol.PackageSymbol) :
        Symbol.ClassSymbol(1L, name, pack) {

    override fun members(): Scope? {
        return Scope(this)
    }

    override fun asType(): Type {
        return object : Type.ClassType(Type.noType, List.nil(), this) {
            override fun getKind() = TypeKind.DECLARED
        }
    }

    override fun getKind() = ElementKind.CLASS

}

internal class KotlinPackageSymbol(name: Name,
                          root: Symbol.PackageSymbol,
                          names: Name.Table,
                          symtab: Symtab,
                          completer: Completer) : Symbol.PackageSymbol(name, root) {

    val kotlinClass: Symbol.ClassSymbol

    init {
        val kotlinClassName = names.fromString("SomeClass2")
        kotlinClass = KotlinClassSymbol(kotlinClassName, this)

        symtab.classes.put(kotlinClassName, kotlinClass)

        this.completer = completer

    }

    override fun getEnclosedElements() = listOf(kotlinClass)

}