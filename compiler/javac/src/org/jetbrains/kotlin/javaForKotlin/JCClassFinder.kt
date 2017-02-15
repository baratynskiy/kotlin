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

package org.jetbrains.kotlin.javaForKotlin

import org.jetbrains.kotlin.ExtendedJavac
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class JCClassFinder : JavaClassFinder {

    override fun findClass(classId: ClassId): JavaClass? {
        ExtendedJavac.findClass(classId.asSingleFqName().asString())?.let { return it }

        return ExtendedJavac.findStandardClass(classId.asSingleFqName().asString())
    }

    override fun findPackage(fqName: FqName): JavaPackage? {
        ExtendedJavac.findPackage(fqName.asString())?.let { return it }

        return ExtendedJavac.findStandardPackage(fqName.asString())
    }

    override fun knownClassNamesInPackage(packageFqName: FqName) = null

}