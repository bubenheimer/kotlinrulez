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
 * A [Fact] bit mask for representation in [org.bubenheimer.rulez.rules.FactVector] and
 * [org.bubenheimer.rulez.state.State]
 */
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
inline class FactMask internal constructor(internal val value: UInt) {
    override fun toString() = value.toString(2)
}

/**
 * Obtain the [FactMask] for a [Fact]
 */
internal fun Fact.toMask() =
    FactMask(1u shl id)
