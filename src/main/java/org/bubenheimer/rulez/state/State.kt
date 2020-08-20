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
public inline class State constructor(public val value: UInt) {
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
    public infix fun and(factVector: FactVector): State = State(value and factVector.value)

    /**
     * Logical/bitwise `or`
     */
    public operator fun plus(factVector: FactVector): State = State(value or factVector.value)

    /**
     * Removes [Fact]s in [factVector] from current state.
     */
    public operator fun minus(factVector: FactVector): State =
        State(value and factVector.value.inv())

    /**
     * Returns `true` iff [fact] is valid in current state.
     */
    @Suppress("unused")
    public operator fun contains(fact: Fact): Boolean = value and fact.mask.value != VOID.value

    /**
     * Returns value of [fact] in current state.
     */
    @Suppress("unused")
    public operator fun get(fact: Fact): Boolean = contains(fact)

    /**
     * Overrides the present value of [fact] with [value]
     */
    @Suppress("unused")
    public operator fun invoke(fact: Fact, value: Boolean): State =
        fact.toVector().let { if (value) plus(it) else minus(it) }

    /**
     * State weight is the number of valid [Fact]s
     */
    @ExperimentalStdlibApi
    public fun weight(): Int = value.countOneBits()

    public companion object {
        /**
         * State with no valid [Fact]s
         */
        public val VOID: State = State(0u)
    }
}
