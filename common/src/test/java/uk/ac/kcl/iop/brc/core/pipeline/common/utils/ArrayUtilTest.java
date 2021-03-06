/*
        Copyright (c) 2015 King's College London

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package uk.ac.kcl.iop.brc.core.pipeline.common.utils;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class ArrayUtilTest {

    @Test
    public void shouldReturnTrueIfStringArrayIsEmpty() {
        String[] array = new String[10];

        assertTrue(ArrayUtil.isStringArrayEmpty(array));
    }

    @Test
    public void shouldReturnFalseIfStringArrayIsNotEmpty() {
        String[] array = new String[10];
        array[0] = "test";
        assertFalse(ArrayUtil.isStringArrayEmpty(array));
    }

    @Test
    public void shouldReturnTrueIfStringArrayIsNull() {
        String[] array = null;

        assertTrue(ArrayUtil.isStringArrayEmpty(array));
    }

}
