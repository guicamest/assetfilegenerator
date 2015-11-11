/*
 * Copyright 2015 guicamest
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
package io.github.guicamest.assetfilegenerator

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class AFGPlugin implements Plugin<Project> {

	void apply(Project project) {
		project.extensions.create('afg', AFGPluginExtension)

		project.afterEvaluate {
			def variants = null
			if (project.android.hasProperty('applicationVariants')) {
				variants = project.android.applicationVariants
			}
			else if (project.android.hasProperty('libraryVariants')) {
				variants = project.android.libraryVariants
			}
			else {
				throw new IllegalStateException('Android project must have applicationVariants or libraryVariants!')
			}
			def inclusions = project.afg.include
			def exclusions = project.afg.exclude - inclusions

			Task eclipseTask = project.getTasks().findByPath('eclipse')
			if ( eclipseTask != null ){
				def sourceOutputDir = new File(project.android.sourceSets.main.java.srcDirs[0].parentFile,'gen')
				def assetsDirs = project.android.sourceSets.main.assets.srcDirs
				def inputs = project.fileTree(assetsDirs[0]).matching{
					exclude(exclusions)
					include(inclusions)
				}
				String packageVar = project.android.defaultConfig.hasProperty('packageName') ? 'packageName' : 'applicationId'
				String packageName = project.android.defaultConfig."$packageVar"
				def generateEclipseAssetsFileTask = project.task("generateEclipseAssetsFile", type: GenerateAssetFileTask) {
					sources = inputs
					it.sourceOutputDir = sourceOutputDir
					it.packageName = packageName
					sourceDir = assetsDirs[0]
				}
				generateEclipseAssetsFileTask.description = 'Generate Assets.java for eclipse build.'
				generateEclipseAssetsFileTask.group = 'IDE'
				eclipseTask.dependsOn generateEclipseAssetsFileTask
			}

			// https://android.googlesource.com/platform/tools/build/+/6d7fd0d2eff092abf1aaf44d03756b24570b390c/gradle/src/main/groovy/com/android/build/gradle/BasePlugin.groovy#457

			// Register our task with the variant's resources
			variants.all { variant ->
				def variantName = variant.name.capitalize()

				//https://android.googlesource.com/platform/tools/build/+/6d7fd0d2eff092abf1aaf44d03756b24570b390c/builder/src/main/java/com/android/builder/internal/BuildConfigGenerator.java
				def sourceOutputDir = project.file("$project.buildDir/generated/source/afg/${variant.dirName}")

				def assetsDir = variant.mergeAssets.outputDir
				def inputs = project.fileTree(assetsDir).matching{
					exclude(exclusions)
					include(inclusions)
				}

				String packageVar = variant.generateBuildConfig.hasProperty('packageName') ? 'packageName' : 'buildConfigPackageName'
				String packageName = variant.generateBuildConfig."$packageVar"

				//https://android.googlesource.com/platform/tools/build/+/6d7fd0d2eff092abf1aaf44d03756b24570b390c/gradle/src/main/groovy/com/android/build/gradle/BasePlugin.groovy#495
				def generateBuildAssetsFileTask = project.task("generate${variantName}AssetsFile", type: GenerateAssetFileTask) {
					sources = inputs
					it.sourceOutputDir = sourceOutputDir
					it.packageName = packageName
					sourceDir = assetsDir
				}
				generateBuildAssetsFileTask.description = "Generate Assets.java for ${variantName} build."

				variant.registerJavaGeneratingTask(generateBuildAssetsFileTask, generateBuildAssetsFileTask.sourceOutputDir)
				generateBuildAssetsFileTask.dependsOn variant.mergeAssets
				//variant.mergeAssets.doLast(generateBuildAssetsFileTask.actions[0])
			}
		}
	}

}