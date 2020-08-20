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
package org.bubenheimer.rulez.facts

import org.bubenheimer.rulez.rules.Conjunction
import org.bubenheimer.rulez.rules.FactVector
import org.bubenheimer.rulez.rules.toVector

/**
 * A basic fact
 *
 * @param factBase scope of [Fact] for [id] allocation
 */
public open class Fact constructor(factBase: FactBase) : Conjunction {
    /**
     * The internal fact id, valid within a given [FactBase]. All facts in a [FactBase]
     * have distinct IDs.
     */
    public val id: Int = factBase.allocateFactId()

    /**
     * Fact mask generated from [id].
     */
    public val mask: FactMask = toMask()

    /**
     * A fact is a [Conjunction] with a single conjunct.
     */
    final override val conjuncts: Iterable<Fact> get() = listOf(this)

    /**
     * Just makes it final as there is no good reason for further overrides.
     */
    final override val disjuncts: Iterable<Conjunction> get() = super.disjuncts

    /**
     * Combine this fact with [fact] into a [org.bubenheimer.rulez.rules.FactVector]
     */
    public operator fun plus(fact: Fact): FactVector = toVector() + fact

    override fun toString(): String = "Fact $id"
}

/**
 * Creates a new [Fact]
 *
 * @receiver scope of [Fact] for [Fact.id] allocation
 */
public fun FactBase.newFact(): Fact = Fact(this)
