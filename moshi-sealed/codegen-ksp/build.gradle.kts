/*
 * Copyright (C) 2020 Zac Sweers
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

plugins {
  id("com.google.devtools.ksp") version Dependencies.Kotlin.Ksp.version
  kotlin("jvm")
  id("com.vanniktech.maven.publish")
}

dependencies {
  implementation(Dependencies.AutoService.annotations)
  ksp(Dependencies.AutoService.ksp)
  // For access to MessageCollectorBasedKSPLogger
  compileOnly(Dependencies.Kotlin.Ksp.ksp)
  compileOnly(Dependencies.Kotlin.Ksp.api)
  compileOnly(Dependencies.Kotlin.compilerEmbeddable)

  implementation(Dependencies.KotlinPoet.kotlinPoet)
  implementation(Dependencies.Moshi.adapters)
  implementation(Dependencies.Moshi.moshi)
  implementation(project(":moshi-sealed:runtime"))

  testImplementation(Dependencies.Kotlin.Ksp.api)
  testImplementation(Dependencies.Testing.truth)
  testImplementation(Dependencies.Testing.junit)
  testImplementation(Dependencies.Kotlin.Ksp.ksp)
  // TODO re-enable with new release
//  testImplementation(Dependencies.Testing.kspCompileTesting)
  testImplementation(Dependencies.Kotlin.Ksp.ksp)
  testImplementation(Dependencies.Testing.compileTesting)
  testImplementation(Dependencies.Kotlin.compilerEmbeddable)
}
