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

package org.jetbrains.kotlin

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import javax.annotation.PostConstruct
import javax.inject.Inject

class ClassFinder : JavaClassFinder {

    lateinit var proj: Project
    lateinit var baseScope: GlobalSearchScope
    lateinit var javac: Javac

    @Inject
    fun setProject(project: Project) {
        proj = project
        javac = project.getComponent(Javac::class.java)
    }

    @Inject
    fun setScope(scope: GlobalSearchScope) {
        baseScope = scope
    }

    override fun findClass(classId: ClassId) = javac.findClass(classId.asSingleFqName().asString())

    override fun findPackage(fqName: FqName) = javac.findPackage(fqName.asString())

    override fun knownClassNamesInPackage(packageFqName: FqName) = null

    @PostConstruct
    fun initialize(trace: BindingTrace, codeAnalyzer: KotlinCodeAnalyzer) {
        CodeAnalyzerInitializer.getInstance(proj).initialize(trace, codeAnalyzer.moduleDescriptor, codeAnalyzer)
    }


}