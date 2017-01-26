/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.ClassDataWithSource
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.kotlin.utils.addToStdlib.check
import javax.inject.Inject

class DeserializedDescriptorResolver {
    lateinit var components: DeserializationComponents

    // component dependency cycle
    @Inject
    fun setComponents(components: DeserializationComponentsForJava) {
        this.components = components.components
    }

    fun resolveClass(kotlinClass: KotlinJvmBinaryClass): ClassDescriptor? {
        val classData = readClassData(kotlinClass) ?: return null
        return components.classDeserializer.deserializeClass(kotlinClass.classId, classData)
    }

    internal fun readClassData(kotlinClass: KotlinJvmBinaryClass): ClassDataWithSource? {
        val data = readData(kotlinClass, KOTLIN_CLASS) ?: return null
        val strings = kotlinClass.classHeader.strings ?: return null
        val classData = parseProto(kotlinClass) {
            JvmProtoBufUtil.readClassDataFrom(data, strings)
        } ?: return null
        val source = KotlinJvmBinarySourceElement(kotlinClass, kotlinClass.incompatibility, kotlinClass.isPreReleaseInvisible)
        return ClassDataWithSource(classData, source)
    }

    fun createKotlinPackagePartScope(descriptor: PackageFragmentDescriptor, kotlinClass: KotlinJvmBinaryClass): MemberScope? {
        val data = readData(kotlinClass, KOTLIN_FILE_FACADE_OR_MULTIFILE_CLASS_PART) ?: return null
        val strings = kotlinClass.classHeader.strings ?: return null
        val (nameResolver, packageProto) = parseProto(kotlinClass) {
            JvmProtoBufUtil.readPackageDataFrom(data, strings)
        } ?: return null
        val source = JvmPackagePartSource(kotlinClass, kotlinClass.incompatibility, kotlinClass.isPreReleaseInvisible)
        return DeserializedPackageMemberScope(descriptor, packageProto, nameResolver, source, components) {
            // All classes are included into Java scope
            emptyList()
        }
    }

    private val KotlinJvmBinaryClass.incompatibility: IncompatibleVersionErrorData<JvmMetadataVersion>?
        get() {
            if (classHeader.metadataVersion.isCompatible()) return null
            return IncompatibleVersionErrorData(classHeader.metadataVersion, JvmMetadataVersion.INSTANCE, location, classId)
        }

    private val KotlinJvmBinaryClass.isPreReleaseInvisible: Boolean
        get() = !JvmMetadataVersion.skipCheck &&
                !IS_PRE_RELEASE &&
                (classHeader.isPreRelease || classHeader.metadataVersion == KOTLIN_1_1_EAP_METADATA_VERSION)

    internal fun readData(kotlinClass: KotlinJvmBinaryClass, expectedKinds: Set<KotlinClassHeader.Kind>): Array<String>? {
        val header = kotlinClass.classHeader
        return (header.data ?: header.incompatibleData)?.check { header.kind in expectedKinds }
    }

    private inline fun <T : Any> parseProto(klass: KotlinJvmBinaryClass, block: () -> T): T? {
        try {
            try {
                return block()
            }
            catch (e: InvalidProtocolBufferException) {
                throw IllegalStateException("Could not read data from ${klass.location}", e)
            }
        }
        catch (e: Throwable) {
            if (!klass.classHeader.metadataVersion.isCompatible()) {
                // TODO: log.warn
                return null
            }
            throw e
        }
    }

    companion object {
        internal val KOTLIN_CLASS = setOf(KotlinClassHeader.Kind.CLASS)

        private val KOTLIN_FILE_FACADE_OR_MULTIFILE_CLASS_PART =
                setOf(KotlinClassHeader.Kind.FILE_FACADE, KotlinClassHeader.Kind.MULTIFILE_CLASS_PART)

        private val KOTLIN_1_1_EAP_METADATA_VERSION = JvmMetadataVersion(1, 1, 2)

        var IS_PRE_RELEASE = KotlinCompilerVersion.IS_PRE_RELEASE
            get() {
                val testOverrideValue = System.getProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY)
                return testOverrideValue?.toBoolean() ?: field
            }
            @Deprecated("Should only be used in tests") set

        const val TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY = "kotlin.test.is.pre.release"
    }
}
