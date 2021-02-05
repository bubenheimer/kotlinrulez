/*
 * Copyright (c) 2015-2020 Uli Bubenheimer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.bubenheimer.rulez.rules

import org.bubenheimer.rulez.facts.Fact

/**
 * A set of [Fact]s
 *
 * @param value a bit vector of [Fact]s, generated via the associated [toVector] methods
 */
public inline class FactVector internal constructor(internal val value: UInt) {
    override fun toString(): String = value.toString(2)

    /**
     * Merges [fact] with FactVector (logical or)
     */
    public operator fun plus(fact: Fact): FactVector = FactVector(value or fact.mask.value)

    /**
     * Removes [fact] from FactVector when present
     */
    public operator fun minus(fact: Fact): FactVector = FactVector(value and fact.mask.value.inv())

    public companion object {
        /**
         * An empty set of Facts
         */
        public val EMPTY: FactVector = FactVector(0u)
    }
}

/**
 * Generates a [FactVector] from a single [Fact]
 */
public fun Fact.toVector(): FactVector = FactVector(mask.value)

/**
 * Generates a [FactVector] from a set of [Fact]s
 */
public fun Iterable<Fact>.toVector(): FactVector = FactVector(
    fold(0u) { vector, fact ->
        vector or fact.mask.value
    }
)
