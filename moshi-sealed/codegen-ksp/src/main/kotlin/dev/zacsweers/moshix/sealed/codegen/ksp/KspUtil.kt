/*
 * Copyright (C) 2021 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.zacsweers.moshix.sealed.codegen.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Visibility
import com.google.devtools.ksp.symbol.Visibility.INTERNAL
import com.google.devtools.ksp.symbol.Visibility.JAVA_PACKAGE
import com.google.devtools.ksp.symbol.Visibility.LOCAL
import com.google.devtools.ksp.symbol.Visibility.PRIVATE
import com.google.devtools.ksp.symbol.Visibility.PROTECTED
import com.google.devtools.ksp.symbol.Visibility.PUBLIC
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier

internal inline fun <reified T> Resolver.getClassDeclarationByName(): KSClassDeclaration {
  return getClassDeclarationByName(T::class.qualifiedName!!)
}

internal fun Resolver.getClassDeclarationByName(fqcn: String): KSClassDeclaration {
  return getClassDeclarationByName(getKSNameFromString(fqcn)) ?: error("Class '$fqcn' not found.")
}

internal fun KSClassDeclaration.asType() = asType(emptyList())

internal fun KSAnnotated.getAnnotationWithType(target: KSType): KSAnnotation {
  return findAnnotationWithType(target) ?: error("Annotation $target not found.")
}

internal inline fun <reified T : Annotation> KSAnnotated.hasAnnotation(
  resolver: Resolver,
): Boolean {
  return hasAnnotation(resolver.getClassDeclarationByName<T>().asType())
}

internal fun KSAnnotated.hasAnnotation(target: KSType): Boolean {
  return findAnnotationWithType(target) != null
}

internal inline fun <reified T : Annotation> KSAnnotated.findAnnotationWithType(
  resolver: Resolver,
): KSAnnotation? {
  return findAnnotationWithType(resolver.getClassDeclarationByName<T>().asType())
}

internal fun KSAnnotated.findAnnotationWithType(target: KSType): KSAnnotation? {
  return annotations.find { it.annotationType.resolve() == target }
}

internal inline fun <reified T> KSAnnotation.getMember(name: String): T {
  val matchingArg = arguments.find { it.name?.asString() == name }
    ?: error(
      "No member name found for '$name'. All arguments: ${arguments.map { it.name?.asString() }}"
    )
  return matchingArg.value as? T ?: error("No value found for $name. Was ${matchingArg.value}")
}

internal fun Visibility.asKModifier(): KModifier {
  return when (this) {
    PUBLIC -> KModifier.PUBLIC
    PRIVATE -> KModifier.PRIVATE
    PROTECTED -> KModifier.PROTECTED
    INTERNAL -> KModifier.INTERNAL
    JAVA_PACKAGE -> KModifier.PUBLIC
    LOCAL -> error("Local is unsupported")
  }
}

internal fun KSAnnotation.toAnnotationSpec(resolver: Resolver): AnnotationSpec {
  val element = annotationType.resolve().declaration as KSClassDeclaration
  // TODO support generic annotations
  val builder = AnnotationSpec.builder(element.toClassName())
  for (argument in arguments) {
    val member = CodeBlock.builder()
    val name = argument.name!!.getShortName()
    member.add("%L = ", name)
    when (val value = argument.value!!) {
      resolver.builtIns.arrayType -> {
//        TODO("Arrays aren't supported tet")
//        member.add("[⇥⇥")
//        values.forEachIndexed { index, value ->
//          if (index > 0) member.add(", ")
//          value.accept(this, name)
//        }
//        member.add("⇤⇤]")
      }
      is KSType -> member.add("%T::class", value.toTypeName())
      // TODO is this the right way to handle an enum constant?
      is KSName ->
        member.add(
          "%T.%L", ClassName.bestGuess(value.getQualifier()),
          value.getShortName()
        )
      is KSAnnotation -> member.add("%L", value.toAnnotationSpec(resolver))
      else -> member.add(memberForValue(value))
    }
    builder.addMember(member.build())
  }
  return builder.build()
}

/**
 * Creates a [CodeBlock] with parameter `format` depending on the given `value` object.
 * Handles a number of special cases, such as appending "f" to `Float` values, and uses
 * `%L` for other types.
 */
internal fun memberForValue(value: Any) = when (value) {
  is Class<*> -> CodeBlock.of("%T::class", value)
  is Enum<*> -> CodeBlock.of("%T.%L", value.javaClass, value.name)
  is String -> CodeBlock.of("%S", value)
  is Float -> CodeBlock.of("%Lf", value)
//  is Char -> CodeBlock.of("'%L'", characterLiteralWithoutSingleQuotes(value)) // TODO public?
  else -> CodeBlock.of("%L", value)
}
