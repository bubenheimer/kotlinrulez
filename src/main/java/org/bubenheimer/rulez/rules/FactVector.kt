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
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
inline class FactVector internal constructor(internal val value: UInt) {
    override fun toString() = value.toString(2)

    /**
     * Merges [fact] with FactVector (logical or)
     */
    operator fun plus(fact: Fact) = FactVector(value or fact.mask.value)

    /**
     * Removes [fact] from FactVector when present
     */
    operator fun minus(fact: Fact) = FactVector(value and fact.mask.value.inv())

    companion object {
        /**
         * An empty set of Facts
         */
        val EMPTY = FactVector(0u)
    }
}

/**
 * Generates a [FactVector] from a single [Fact]
 */
fun Fact.toVector() = FactVector(mask.value)

/**
 * Generates a [FactVector] from a set of [Fact]s
 */
fun Iterable<Fact>.toVector() = FactVector(
    fold(0u) { vector, fact ->
        vector or fact.mask.value
    }
)
