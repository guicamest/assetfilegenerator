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
import com.squareup.javapoet.*
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.LoaderClassPath
import javassist.bytecode.LocalVariableAttribute
import javassist.bytecode.MethodInfo
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import javax.lang.model.element.Modifier
/**
 * Task that generates Assets.java based on /assets folder.
 */
class GenerateAssetFileTask extends DefaultTask {

    /**
     * Input assets
     */
    @InputFiles
    FileCollection sources

    @OutputDirectory
    File sourceOutputDir

    @Input
    String packageName

    File sourceDir

    File androidJar

    @TaskAction
    def generateAssetsFile(IncrementalTaskInputs inputs) {
        logger.info('Generating assets file...')
        logger.info('Package {}', packageName)
        List<File> assetFiles = []

        logger.info('Writing to {}',sourceOutputDir.absolutePath);
        boolean changed = false

        if ( !inputs.isIncremental() ){
            // Everything is outofdate -> added, modified and removed are false
            inputs.outOfDate {
                assetFiles << it.file
            }
            changed = true
        }else {
            inputs.outOfDate {
                assetFiles << it.file
                changed = true
            }
            inputs.removed {
                assetFiles -= it.file
                changed = true
            }
        }
        if ( !changed ){
            return;
        }

        if ( sourceOutputDir.exists()) {
            logger.debug('Deleting Assets.java ...')
            sourceOutputDir.deleteDir()
        }

        TypeSpec.Builder assetsBuilder = TypeSpec.classBuilder('Asset')
            .addModifiers(Modifier.PUBLIC)
            .addField(String.class, 'path', Modifier.PRIVATE, Modifier.FINAL)
            .addField(int.class, 'depth', Modifier.PRIVATE, Modifier.FINAL)

        addConstructor(assetsBuilder)
        addGetPathMethod(assetsBuilder)
        addGetFilenameMethod(assetsBuilder)

        // "Copy" AssetManager open methods
        ClassPool classPool = new ClassPool(ClassPool.getDefault())
        LoaderClassPath loaderClassPath = new LoaderClassPath(new URLClassLoader ([androidJar.toURL()] as URL[], Thread.currentThread().getContextClassLoader()))
        classPool.appendClassPath(loaderClassPath)
        CtClass assetManagerClass = classPool.get('android.content.res.AssetManager')

        assetManagerClass.getDeclaredMethods().findAll{ CtMethod m ->
            m.name.startsWith('open')
        }.each {
            addMethod(assetsBuilder, it)
        }

        def resolvedHierarchy = new RelativeFileHierarchy().resolve(assetFiles.collect{
            [file: it, alias: getAssetAlias(it.name),
             relativePath: getRelativePath(it)]
        })

        addAssetsToClassSpec(resolvedHierarchy, assetsBuilder, ClassName.get(packageName, 'Asset'))

        JavaFile.builder(packageName, assetsBuilder.build()).build().writeTo(sourceOutputDir)
    }

    def addAssetsToClassSpec(def rfh, TypeSpec.Builder classSpec, TypeName assetClassName){
        rfh.files?.each { af ->
            logger.info('Adding asset entry for {}\nRelative path {}',af.file.absolutePath, af.relativePath)
            // For each asset to be included, add an enum
            classSpec.addField(FieldSpec
                    .builder(assetClassName, af.alias, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer('new $T($S, $L)', assetClassName, af.relativePath, af.relativePath.split('/').length)
                    .build())
        }
        rfh.dirs?.each { entry ->
            TypeSpec.Builder classBuilder = TypeSpec.interfaceBuilder(getAssetAlias(entry.key))
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            addAssetsToClassSpec(entry.value, classBuilder, assetClassName)
            classSpec.addType(classBuilder.build())
        }
    }

    String getAssetAlias(String name) {
        name = (name.trim() ?: 'EMPTY').toUpperCase(Locale.ROOT)

        int suffixStart = name.lastIndexOf '.'
        name = suffixStart == -1 ? name : name.substring(0, suffixStart)

        new String(name.collect{ it ->
            (Character.isJavaIdentifierPart(it as char) ? it : '$') as char
        } as char[])
    }

    String getRelativePath(File assetFile){
        // https://gist.github.com/ysb33r/5804364
        sourceDir.toURI().relativize( assetFile.toURI() ).toString()
    }

    def addConstructor(TypeSpec.Builder builder) {
        builder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(String.class, 'path')
                .addParameter(int.class, 'depth')
                .addStatement('this.$N = $N', 'path', 'path')
                .addStatement('this.$N = $N', 'depth', 'depth').build())
    }

    def addGetPathMethod(TypeSpec.Builder builder) {
        builder.addMethod(MethodSpec.methodBuilder('getPath')
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement('return path').build())
    }

    def addGetFilenameMethod(TypeSpec.Builder builder) {
        builder.addMethod(MethodSpec.methodBuilder('getFilename')
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement('return path.split($S)[depth-1]', '/')
                .build())
    }

    def addMethod(TypeSpec.Builder builder, CtMethod method) {
        def mSpec = MethodSpec.methodBuilder(method.name)
                .addParameter(ClassName.get('android.content.res','AssetManager'), 'am')
                .addModifiers(Modifier.PUBLIC)

        def withContextSpec = MethodSpec.methodBuilder(method.name)
                .addParameter(ClassName.get('android.content','Context'), 'ctx')
                .addModifiers(Modifier.PUBLIC)

        method.getExceptionTypes().each{ CtClass cc ->
            ClassName exceptionClass = ClassName.get(cc.packageName, cc.simpleName)
            mSpec.addException(exceptionClass)
            withContextSpec.addException(exceptionClass)
        }

        method.getReturnType().with{ CtClass cc ->
            ClassName returnClass = ClassName.get(cc.packageName, cc.simpleName)
            mSpec.returns(returnClass)
            withContextSpec.returns(returnClass)
        }
        int fileNameIdx = -1;

        MethodInfo methodInfo = method.getMethodInfo();
        LocalVariableAttribute table = methodInfo.getCodeAttribute().getAttribute(LocalVariableAttribute.tag);
        int numberOfLocalParameters = method.parameterTypes.length;
        List<String> parameterNames = (1..numberOfLocalParameters).collect{
            //methodInfo.getConstPool().getUtf8Info(table.nameIndex(it))
            def paramName = table.variableName(it)
            if ( 'fileName'.equals(paramName) ){
                fileNameIdx = it - 1
            }
            paramName
        }

        method.parameterTypes.eachWithIndex{ p, i ->
            if ( i == fileNameIdx ){
                return
            }
            TypeName paramClass = getParameterType(p)
            ParameterSpec ps = ParameterSpec.builder(paramClass, parameterNames[i], Modifier.FINAL).build()
            mSpec.addParameter(ps)
            withContextSpec.addParameter(ps)
        }

        parameterNames[fileNameIdx] = 'getPath()'
        mSpec.addStatement('return am.$L($L)', method.name, parameterNames.join(', '))

        parameterNames.remove(fileNameIdx)
        withContextSpec.addStatement('return $L($L)', method.name, (['ctx.getAssets()'] + parameterNames).join(', '))

        builder.addMethod(mSpec.build()).addMethod(withContextSpec.build())
    }

    TypeName getParameterType(CtClass ctClass) {
        if ( !ctClass.isPrimitive() ){
            return ClassName.get(ctClass.packageName, ctClass.simpleName)
        }
        return TypeName."${ctClass.name.toUpperCase()}"
    }
}
