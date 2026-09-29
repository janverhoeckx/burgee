package io.github.janverhoeckx.burgee.flag.domain

/** The outcome of evaluating one Feature Flag against one Evaluation Context. */
data class Evaluation(val flagKey: String, val result: Boolean)
