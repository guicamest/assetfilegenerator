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
package io.github.guicamest.assetfilegenerator;

class RelativeFileHierarchy {

    public def resolve(def files) {
        doResolve(files.collect{[f:it, splittedPath: it.relativePath.split('/')]})
    }

    private def doResolve(List filesWithSplittedPath){
        def baseFiles = filesWithSplittedPath.findAll{it.splittedPath.length == 1}?:null
        filesWithSplittedPath-= baseFiles
        def dirs = filesWithSplittedPath.groupBy({ fs -> fs.splittedPath[0] })
        [files:baseFiles?.collect{it.f},
         dirs: dirs.collectEntries{ entry ->
             [entry.key, doResolve(entry.value.collect{
                 [f: it.f, splittedPath: Arrays.copyOfRange(it.splittedPath,1,it.splittedPath.length)]
             })]
         }?:null
        ]
    }
}
