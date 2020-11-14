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

/**
 * A named [Fact]
 *
 * @param factBase scope of [NamedFact] for [NamedFact.id] allocation
 * @param name the fact name
 */
public open class NamedFact(factBase: FactBase, public val name: String) : Fact(factBase) {
    override fun toString(): String = "$id: $name"
}

/**
 * Creates a new [NamedFact]
 *
 * @receiver scope of [NamedFact] for [NamedFact.id] allocation
 * @param name a fact name
 */
public fun FactBase.newFact(name: String): NamedFact = NamedFact(this, name)
