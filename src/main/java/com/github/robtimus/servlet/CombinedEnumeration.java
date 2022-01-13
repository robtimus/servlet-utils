/*
 * CombinedEnumeration.java
 * Copyright 2021 Rob Spoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robtimus.servlet;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

final class CombinedEnumeration implements Enumeration<String> {

    private final Enumeration<String> enumeration;
    private final Set<String> additional;
    private Iterator<String> iterator;

    CombinedEnumeration(Enumeration<String> enumeration, Set<String> additional) {
        this.enumeration = enumeration;
        this.additional = new LinkedHashSet<>(additional);
    }

    @Override
    public boolean hasMoreElements() {
        if (enumeration.hasMoreElements()) {
            // Still iterating over the enumeration
            return true;
        }
        if (iterator == null) {
            // Ended iterating over the enumeration, starting iterating over the set
            iterator = additional.iterator();
        }
        return iterator.hasNext();
    }

    @Override
    public String nextElement() {
        if (enumeration.hasMoreElements()) {
            // Still iterating over the enumeration
            String next = enumeration.nextElement();
            additional.remove(next);
            return next;
        }
        if (iterator == null) {
            // Ended iterating over the enumeration, starting iterating over the set
            iterator = additional.iterator();
        }
        return iterator.next();
    }
}
