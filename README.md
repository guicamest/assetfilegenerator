Asset File Generator (AFG) [![Build Status](https://travis-ci.org/guicamest/assetfilegenerator.svg?branch=master)](https://travis-ci.org/guicamest/assetfilegenerator/branches) [ ![JCenter](https://api.bintray.com/packages/guicamest/maven/assetfilegenerator/images/download.svg) ](https://bintray.com/guicamest/maven/assetfilegenerator/\_latestVersion)
======

*That's our best asset! - Somebody*

Refer to your assets directly from your code!

With this plugin, you can reference to your assets (via Asset) in your source code just like you do with your resources (via R).

Usage
-----

Add the following to your build.gradle`:

```gradle
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'io.github.guicamest:assetfilegenerator:0.1.0'
    }
}

apply plugin: 'com.android.application'

// Make sure to apply this plugin *after* the Android plugin
apply plugin: 'io.github.guicamest.assetfilegenerator'
```

AFG adds the `afg` extension to your build. You can define which files are to be included/excluded from the generated file:

```gradle
afg {
    include=[*.sql,*.txt]
    exclude=[]
}

```

Also, your Android Gradle plugin (`com.android.tools.build:gradle`) must be at least version 1.1.+

Sample
------

You can find a sample application under the *sample* folder.

Known Issues
------------

- AFG needs JDK 1.7+ to generate the source code. If you don't have it already, install and use [JDK 1.7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html) for your instance of Android Studio. For instructions how, consult [this article](https://intellij-support.jetbrains.com/entries/23455956-Selecting-the-JDK-version-the-IDE-will-run-under).


- Android Studio doesn't automatically rebuild if the assets folder is modified (like it does with other resources). Therefore, if you add/remove resources you will have to manually rebuild before they appear in Asset.java

Planned Features
----------------

- Suggestions are welcome
