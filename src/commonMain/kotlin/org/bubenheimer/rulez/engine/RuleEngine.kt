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
package org.bubenheimer.rulez.engine

import org.bubenheimer.rulez.rules.Rule
import org.bubenheimer.rulez.state.FactState
import org.bubenheimer.rulez.state.State

/**
 * Abstract rule engine missing an evaluation strategy.
 *
 * @param factState the fact state (bit vector)
 * @param rules the rule base
 * @param evalStalledHandler called if evaluation stalls; may throw [EvalStalledException]
 *
 */
public abstract class RuleEngine(
    protected val factState: FactState,
    rules: Iterable<Rule>,
    private val evalStalledHandler: EvalStalledHandler? = null
) {
    /**
     * Copy of passed rules
     */
    protected val rules: List<Rule> = rules.toList()

    /**
     * To be called by subclasses when evaluation has moved into a stalled state.
     *
     * @throws EvalStalledException to terminate evaluation with an error
     */
    @Throws(EvalStalledException::class)
    protected fun handleEvaluationStall() {
        evalStalledHandler?.invoke(factState.state)
    }
}

/**
 * May be thrown by user code when evaluation has moved into a stalled state.
 * This will generally terminate rule engine operations.
 */
public class EvalStalledException(message: String? = null) : Exception(message)

/**
 * Called when rule evaluation stalls.
 *
 * This is a functional interface rather than a typealias primarily as an optimization to avoid
 * boxing upon invocation.
 */
public fun interface EvalStalledHandler {
    /**
     * @param state the current fact [State]
     *
     * @throws EvalStalledException to terminate evaluation with an error
     */
    @Throws(EvalStalledException::class)
    public fun invoke(state: State)
}
