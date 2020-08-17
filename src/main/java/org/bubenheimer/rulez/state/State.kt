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

package org.bubenheimer.rulez.state

import org.bubenheimer.rulez.facts.Fact
import org.bubenheimer.rulez.rules.FactVector
import org.bubenheimer.rulez.rules.toVector

/**
 * Rule engine fact state
 */
inline class State constructor(val value: UInt) {
    /**
     * Convenience method to format the fact state of the rule engine or the rule base
     * evaluation state as a string in a standard manner (a reversed bit vector string).
     * @return the standardized string-formatted state
     */
    override fun toString(): String = value.toString(2)

    /**
     * Returns `true` iff fact state contains all [Fact]s in [factVector].
     */
    internal fun matches(factVector: FactVector): Boolean =
        value and factVector.value == factVector.value

    /**
     * Logical/bitwise `and`
     */
    infix fun and(factVector: FactVector) = State(value and factVector.value)

    /**
     * Logical/bitwise `or`
     */
    operator fun plus(factVector: FactVector) = State(value or factVector.value)

    /**
     * Removes [Fact]s in [factVector] from current state.
     */
    operator fun minus(factVector: FactVector) = State(value and factVector.value.inv())

    /**
     * Returns `true` iff [fact] is valid in current state.
     */
    @Suppress("unused")
    operator fun contains(fact: Fact) = value and fact.mask.value != VOID.value

    /**
     * Returns value of [fact] in current state.
     */
    @Suppress("unused")
    operator fun get(fact: Fact) = contains(fact)

    /**
     * Overrides the present value of [fact] with [value]
     */
    @Suppress("unused")
    operator fun invoke(fact: Fact, value: Boolean) =
        fact.toVector().let { if (value) plus(it) else minus(it) }

    /**
     * State weight is the number of valid [Fact]s
     */
    @ExperimentalStdlibApi
    fun weight() = value.countOneBits()

    companion object {
        /**
         * State with no valid [Fact]s
         */
        val VOID = State(0u)
    }
}
