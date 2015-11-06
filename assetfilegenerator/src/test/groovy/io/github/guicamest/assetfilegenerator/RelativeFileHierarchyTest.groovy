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

import org.junit.Before;
import org.junit.Test;

class RelativeFileHierarchyTest {

    def rfh
    @Before
    public void setup(){
        rfh = new RelativeFileHierarchy()
    }

    @Test
    public void simpleTest(){
        def files = ['bla','ble'].collect {
            [relativePath: it]
        }
        Collections.shuffle(files)

        def resolved = rfh.resolve(files)
        assert resolved.files*.relativePath.containsAll(['bla', 'ble'])
        assert resolved.dirs == null
    }

    @Test
    public void simpleDirTest(){
        def files = ['a/bla','a/ble'].collect {
            [relativePath: it]
        }
        Collections.shuffle(files)

        def resolved = rfh.resolve(files)
        assert resolved.files*.relativePath == null
        def dirs = resolved.dirs
        assert dirs['a'].files*.relativePath.containsAll(['a/bla', 'a/ble'])
        assert dirs['a'].dirs == null
    }

    @Test
    public void largeTest(){
        def files = ['bla','ble','a/bla','a/ble','b/bla','c/bla','c/a/bla','c/b/bla','d/a/bla','d/b/bla'].collect {
            [relativePath: it]
        }
        Collections.shuffle(files)

        def resolved = rfh.resolve(files)
        assert resolved.files*.relativePath.containsAll(['bla', 'ble'])

        def dirs = resolved.dirs
        assert dirs['a'].files*.relativePath.containsAll(['a/bla', 'a/ble'])
        assert dirs['a'].dirs == null
        assert dirs['b'].files*.relativePath.containsAll(['b/bla'])
        assert dirs['b'].dirs == null

        assert dirs['c'].files*.relativePath.containsAll(['c/bla'])
        def cdirs = dirs['c'].dirs
        assert cdirs['a'].files*.relativePath.containsAll(['c/a/bla'])
        assert cdirs['a'].dirs == null
        assert cdirs['b'].files*.relativePath.containsAll(['c/b/bla'])
        assert cdirs['b'].dirs == null

        assert dirs['d'].files == null
        def ddirs = dirs['d'].dirs
        assert ddirs['a'].files*.relativePath.containsAll(['d/a/bla'])
        assert ddirs['a'].dirs == null
        assert ddirs['b'].files*.relativePath.containsAll(['d/b/bla'])
        assert ddirs['b'].dirs == null
    }
}
