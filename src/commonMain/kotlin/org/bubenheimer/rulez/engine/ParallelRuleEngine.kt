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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.bubenheimer.rulez.rules.ActionResult
import org.bubenheimer.rulez.rules.Rule
import org.bubenheimer.rulez.state.FactState
import org.bubenheimer.rulez.state.State
import org.bubenheimer.util.BitSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException

/**
 * A rule engine where conceptually the evaluation strategy checks for each rule whether its
 * left-hand side matches the current state, and, if so, fires the rule, executing the rule action.
 * Rule actions run as individual coroutines, offering a potentially large degree of parallelism and
 * also a potentially large degree of execution isolation, depending on the execution
 * environment. Execution results are collected in batches, applied to the rule engine state,
 * and used as the basis for firing additional matching rules in the described manner.
 * This results in a kind of forward-chaining, parallel breadth-first rule evaluation strategy.
 *
 * The actual implementation may use optimizations such that the conceptual strategy is
 * implemented in a non-literal, yet equivalent manner.
 *
 * @param factState The initial fact state
 * @param rules The rules. Rules are copied for use in the rule engine.
 * @param evalStalledHandler Called with the final [State] once evaluation ends due to a stable fact
 * state
 * @param evalIterationListener Called with the new [State] at the beginning and
 * at the end of each evaluation iteration; the end state may not be the same as the next
 * iteration's begin state as other coroutines may add and remove facts in-between. The passed
 * [Boolean] is `false` when called at the beginning of an eval, and `true` at the end.
 * @param evalLogger Called with log output at various points during evaluation. The logger should
 * not be set for performance-critical code as generating log output incurs significant overhead.
 */
@ExperimentalContracts
public open class ParallelRuleEngine(
    factState: FactState,
    rules: Iterable<Rule>,
    evalStalledHandler: EvalStalledHandler? = null,
    private val evalIterationListener: EvalIterationListener? = null,
    private val evalLogger: EvalLogger? = null
) : RuleEngine(factState, rules, evalStalledHandler) {
    /**
     * Internal result from rule action combined with the rule's index for recordkeeping.
     */
    private sealed class IndexedResult {
        abstract val ruleIndex: Int?

        internal class Success(val result: ActionResult, override val ruleIndex: Int?) :
            IndexedResult()

        internal class Failure(val t: Throwable, override val ruleIndex: Int) : IndexedResult()
    }

    /**
     * The evaluation state of all rules. A rule with a currently executing action is `true`,
     * otherwise `false`.
     */
    private val rulesState = BitSet(rules.count())

    /**
     * Receives successful and failed results from rule action execution. The specified channel
     * buffer size is virtually guaranteed to ensure non-suspending operation.
     */
    private val channel = Channel<IndexedResult>(rules.count())

    /**
     * Wraps rule action to handle failures and send results to channel.
     */
    @Suppress("SuspendFunctionOnCoroutineScope") // warning not applicable
    private suspend inline fun CoroutineScope.actionWrapper(
        ruleIndex: Int,
        @Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE") // nonsense warning
        block: suspend CoroutineScope.() -> ActionResult
    ) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        try {
            val actionResult = block()
            channel.send(IndexedResult.Success(actionResult, ruleIndex))
        } catch (t: Throwable) {
            withContext(NonCancellable) {
                channel.send(IndexedResult.Failure(t, ruleIndex))
            }
            // Re-throw to ensure standard handling of cancellations and errors
            throw t
        }
    }

    /**
     * Tracks the most recently completed rule to shift the evaluation bias in a predictable
     * fashion; this is useful when a rule keeps getting fired for various reasons (exception,
     * cancellation, etc.), avoiding starvation of other rules.
     */
    private var lastCompletedRuleIndex = -1

    /**
     * Apply [org.bubenheimer.rulez.facts.Fact]s from an external source. This should generally be
     * avoided, as it makes rule engine operation less predictable. In particular, it becomes much
     * harder to reliably detect a stalled rule engine. However, the method can be useful when
     * rule execution has to rely on external process flow, for example on Android:
     *
     *    NamedRule("Show onboarding dialog",
     *        givenNot(onboardingDialogShown or onboardingDialogShowing) then action {
     *            OnboardingDialogFragment().showNow(
     *            childFragmentManager, TAG_FRAGMENT_ONBOARDING_DIALOG
     *            )
     *            ActionResult(add = onboardingDialogShowing)
     *            // The Dialog operates on its own outside the engine from here on; we tie things
     *            // back to the engine by calling the following once the dialog is dismissed:
     *            // ParallelRuleEngine.applyExternalFacts(ActionResult(
     *            //     remove = onboardingDialogShowing, add = onboardingDialogShown))
     *        }
     *    )
     *
     *
     * A good alternative can be the use of [CompletableJob] and [CompletableDeferred], which can be
     * part of a rule action to suspend, and can be completed externally to let execution resume.
     * For example:
     *
     *    val onboardingDialogDismissed = Job()
     *
     *    NamedRule("Show onboarding dialog",
     *        givenNot(onboardingDialogShown or onboardingDialogShowing) then action {
     *            OnboardingDialogFragment().showNow(
     *                childFragmentManager, TAG_FRAGMENT_ONBOARDING_DIALOG
     *            )
     *            ActionResult(add = onboardingDialogShowing)
     *            // The Dialog operates on its own outside this engine from here on; we tie things
     *            // back to the engine with a CompletableJob in the rule below
     *        }
     *    ),
     *    NamedRule("Process onboarding dialog dismissal",
     *        given(onboardingDialogShowing) then action {
     *            // Call CompletableJob.complete() externally once dialog is dismissed
     *            onboardingDialogDismissed.join()
     *            ActionResult(remove = onboardingDialogShowing, add = onboardingDialogShown)
     *        }
     *    )
     */
    public suspend fun applyExternalFacts(actionResult: ActionResult) {
        channel.send(IndexedResult.Success(actionResult, null))
    }

    /**
     * Evaluates the rule base. This should be called only once per class instance.
     * Rule engine evaluation is terminated cleanly by cancelling its parent `Job`. The coroutine
     * context in which this method is called influences the behavior of rule action coroutines.
     * For example, calling from inside `supervisorScope` can run the individual rule action
     * coroutines in a more isolated manner and makes them more resilient to failures in other rule
     * action coroutines. Depending on the execution environment, this may require additional
     * boilerplate. For example, on Android, it can be advisable to define a
     * `CoroutineExceptionHandler` to not let a rule action failure kill the process:
     *
     *     val job = lifecycleOwner.lifecycleScope.launchWhenStarted {
     *         withContext(CoroutineExceptionHandler { _, t -> Log.e(TAG, "error", t) }) {
     *             supervisorScope {
     *                 try {
     *                     ruleEngine.evaluate()
     *                 } finally {
     *                     Log.d(TAG, "Rule engine evaluation complete")
     *                 }
     *             }
     *         }
     *     }
     *
     * @throws EvalStalledException when evaluation has stalled
     */
    @Throws(EvalStalledException::class, CancellationException::class)
    public suspend fun evaluate(): Unit = try {
        // Make a CoroutineScope available for creating new coroutines
        coroutineScope {
            // rule engine evaluation loop; terminates if coroutine got cancelled
            while (isActive) {
                evalIterationListener?.invoke(factState.state)

                if (matchRules() == 0) {
                    // May throw EvalStalledException
                    handleEvaluationStall()
                }

                // yield() to other coroutines for more reliable operation when some rule is
                // misbehaving with Dispatchers.Main.immediate or other challenging dispatchers and
                // environments
                yield()

                consumeResults()
            }
        }
    } finally {
        channel.close()
    }

    /**
     * Evaluates not currently fired rules against the current knowledge base state
     * and fires successfully matched rule actions as new coroutines.
     *
     * @return the current number of rule actions in progress
     */
    private fun CoroutineScope.matchRules(): Int {
        // Cycle initialization order - here is the idea, but it causes plenty of implementation
        // overhead as is:
        //
        // return ((lastCompletedRuleIndex + 1 until rules.size) + (0..lastCompletedRuleIndex))
        //     .count { matchRule(it) }
        //
        // Instead, as an optimization, use 2 distinct ranges and basic for loops to avoid Iterable,
        // etc.

        var rulesInProgress = 0

        for (i in lastCompletedRuleIndex + 1 until rules.size) {
            if (matchRule(i)) ++rulesInProgress
        }

        for (i in 0..lastCompletedRuleIndex) {
            if (matchRule(i)) ++rulesInProgress
        }

        return rulesInProgress
    }

    /**
     * Evaluates rule against the current knowledge base state.
     * If successfully matched, fires rule action as new coroutine.
     *
     * This could be an internal function to [matchRules], but is not as an optimization.
     *
     * @return `true` iff match successful
     */
    private fun CoroutineScope.matchRule(ruleIndex: Int): Boolean =
        rulesState[ruleIndex].takeIf { it }?.also {
            evalLogger?.invoke("Rule ${ruleIndex + 1} running: ${rules[ruleIndex]}")
        } ?: run {
            val rule = rules[ruleIndex]
            if (rule.eval(factState.state)) {
                evalLogger?.invoke("Rule ${ruleIndex + 1} firing: $rule")
                val ruleAction = rule.action
                launch(ruleAction.context) { actionWrapper(ruleIndex, ruleAction.block) }
                rulesState[ruleIndex] = true
                true
            } else {
                false
            }
        }

    /**
     * Consumes results from rule action execution.
     */
    private suspend fun consumeResults() {
        //TODO optimization: check if the factState actually changed

        // loop never suspends while channel is not empty, but purging channel and processing
        // contents is swift
        do {
            channel.receive().apply {
                ruleIndex?.let {
                    lastCompletedRuleIndex = it
                    rulesState[it] = false
                }

                when (this) {
                    is IndexedResult.Failure -> evalLogger?.invoke(
                        "Rule ${ruleIndex + 1} (\"${rules[ruleIndex]}\") terminated with error: $t"
                    )

                    is IndexedResult.Success -> {
                        evalLogger?.invoke(ruleIndex?.let {
                            "Applying rule ${it + 1} result: $result"
                        } ?: "Applying external state change: $result"
                        )

                        factState.removeAddFacts(
                            remove = result.removeVector,
                            add = result.addVector
                        )
                    }
                }
            }
        } while (!channel.isEmpty)
    }
}

/**
 * Called at the beginning of each evaluation iteration.
 *
 * This is a functional interface rather than a typealias primarily as an optimization to avoid
 * boxing upon invocation.
 */
public fun interface EvalIterationListener {
    /**
     * @param state the current fact [State]
     */
    public fun invoke(state: State)
}

/**
 * Called at various points during evaluation to log debug messages
 *
 * @param message a debug message [String] to be printed
 */
public typealias EvalLogger = (message: String) -> Unit
