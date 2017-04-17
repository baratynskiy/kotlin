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

package org.jetbrains.kotlin.wrappers.trees

import com.sun.source.util.TreePath
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.javac.MockKotlinClassifier
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.name.FqName

fun TreePath.resolve(javac: JavacWrapper): JavaClassifier? {
    val name = leaf.toString().substringBefore("<").substringAfter("@")
    val nameParts = name.split(".")

    with(compilationUnit as JCTree.JCCompilationUnit) {
        tryToResolveInner(name, javac, nameParts)?.let { return it }
        tryToResolvePackageClass(name, javac, nameParts)?.let { return it }
        tryToResolveByFqName(name, javac)?.let { return it }
        tryToResolveSingleTypeImport(name, javac, nameParts)?.let { return it }
        tryToResolveTypeImportOnDemand(name, javac, nameParts)?.let { return it }
        tryToResolveInJavaLang(name, javac)?.let { return it }
    }

    return tryToResolveTypeParameter(javac)
}

private fun TreePath.tryToResolveInner(name: String,
                                       javac: JavacWrapper,
                                       nameParts: List<String> = emptyList()) = findEnclosingClass(javac)?.findInner(name, javac, nameParts)

private fun JavaClass.findInner(name: String,
                                javac: JavacWrapper,
                                nameParts: List<String> = emptyList()) : JavaClass? {
    if (nameParts.size > 1) {
        nameParts.drop(1).fold(javac.findClass(FqName("${fqName!!.asString()}.${nameParts[0]}")) ?: return null) {
            javaClass, it -> javaClass.findInner(it, javac) ?: return null
        }.let { return it }
    } else {
        if (name == this.fqName?.shortName()?.asString()) return this
        javac.findClass(FqName("${fqName!!.asString()}.$name"))?.let { return it }

        supertypes
                .mapNotNull { it.classifier as? JavaClass }
                .filter { it !is MockKotlinClassifier }
                .forEach { it.findInner(name, javac)?.let { return it } }
    }

    return null
}

private fun TreePath.findEnclosingClass(javac: JavacWrapper) = filterIsInstance<JCTree.JCClassDecl>()
        .filter { it.extending != leaf && !it.implementing.contains(leaf) }
        .reversed()
        .joinToString(separator = ".", prefix = "${compilationUnit.packageName}.") { it.simpleName }
        .let { javac.findClass(FqName(it)) }

private fun JCTree.JCCompilationUnit.tryToResolveSingleTypeImport(name: String,
                                                                  javac: JavacWrapper,
                                                                  nameParts: List<String> = emptyList()): JavaClass? {
    if (nameParts.size > 1) {
        val foundImports = imports.filter { it.qualifiedIdentifier.toString().endsWith(".${nameParts.first()}") }
        foundImports.forEach {
            nameParts.drop(1).fold(javac.findClass(FqName("${it.qualifiedIdentifier}")) ?: return null) {
                javaClass, it -> javaClass.findInner(it, javac) ?: return null
            }.let { return it }
        }
        return null
    } else return imports
            .find { it.qualifiedIdentifier.toString().endsWith(".$name") }
            ?.let {
                it.qualifiedIdentifier.toString()
                        .let(::FqName)
                        .let { javac.findClass(it) ?: javac.getKotlinClassifier(it) }
            }
}

private fun JCTree.JCCompilationUnit.tryToResolvePackageClass(name: String,
                                                              javac: JavacWrapper,
                                                              nameParts: List<String> = emptyList()): JavaClass? {
    if (nameParts.size > 1) {
        nameParts.drop(1).fold(javac.findClass(FqName("$packageName.${nameParts.first()}")) ?: return null) {
            javaClass, it -> javaClass.findInner(it, javac) ?: return null
        }.let { return it }
    } else return (javac.findClass(FqName("$packageName.$name")) ?: javac.getKotlinClassifier(FqName("$packageName.$name")))
            ?.let { return it }
}

private fun JCTree.JCCompilationUnit.tryToResolveTypeImportOnDemand(name: String,
                                                                    javac: JavacWrapper,
                                                                    nameParts: List<String> = emptyList()): JavaClass? {
    val packagesWithAsterisk = imports.filter { it.qualifiedIdentifier.toString().endsWith("*") }

    if (nameParts.size > 1) {
        packagesWithAsterisk.forEach { pack ->
            nameParts.drop(1).fold(javac.findClass(FqName("${pack.qualifiedIdentifier.toString().substringBefore("*")}${nameParts.first()}")) ?: return null) {
                javaClass, it -> javaClass.findInner(it, javac) ?: return null
            }.let { return it }
        }
        return null
    } else {
        packagesWithAsterisk
                .forEach {
                    val fqName = "${it.qualifiedIdentifier.toString().substringBefore("*")}$name".let(::FqName)
                    javac.findClass(fqName)?.let { return it } ?: javac.getKotlinClassifier(fqName)?.let { return it }
                }

        return null
    }
}

private fun tryToResolveByFqName(name: String,
                                 javac: JavacWrapper) = javac.findClass(FqName(name))

private fun tryToResolveInJavaLang(name: String,
                                   javac: JavacWrapper) = javac.findClass(FqName("java.lang.$name"))

private fun TreePath.tryToResolveTypeParameter(javac: JavacWrapper) = filter { it is JCTree.JCClassDecl || it is JCTree.JCMethodDecl }
        .flatMap {
            when (it) {
                is JCTree.JCClassDecl -> it.typarams
                is JCTree.JCMethodDecl -> it.typarams
                else -> emptyList<JCTree.JCTypeParameter>()
            }
        }
        .find { it.toString().substringBefore(" ") == leaf.toString() }
        ?.let {
            JCTypeParameter(it,
                            javac.getTreePath(it, compilationUnit),
                            javac)
        }