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

import org.bubenheimer.rulez.rules.FactVector

/**
 * The current semantic state of the rule engine (what's true and what's false)
 *
 * @param state The initial state
 * @param changeListener Called whenever a potential state change occurs.
 */
public class FactState(
    state: State = State.VOID,
    private val changeListener: ChangeListener? = null
) {
    /**
     * The current semantic state
     */
    public var state: State = state
        private set

    /**
     * Adds facts to the state and removes facts from the state via two fact bit vectors.
     * @param remove   the facts to remove
     * @param add      the facts to add
     * @return `true` iff the state changed
     */
    internal fun removeAddFacts(
        remove: FactVector,
        add: FactVector
    ): Boolean {
        val oldState = state
        state = state - remove + add
        changeListener?.invoke(oldState, remove, add, state)
        return oldState != state
    }
}

/**
 * Called whenever a potential state change occurs. The function parameters
 * are the old state, added facts, removed facts, and new state, in this order.
 */
public typealias ChangeListener = (
    oldState: State,
    removedFacts: FactVector,
    addedFacts: FactVector,
    newState: State
) -> Unit
