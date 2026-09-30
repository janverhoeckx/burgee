package io.github.janverhoeckx.burgee.flag.application.service

import io.github.janverhoeckx.burgee.flag.domain.FeatureFlag

/** Builds the human-readable `detail` text of the audit entries recorded for flag changes. */
internal object FlagAuditDetails {

    fun creation(flag: FeatureFlag): String {
        val rule = flag.targetingRule
        val targeting = if (rule.conditions.isEmpty()) "" else ", conditions=${rule.describe()}"
        return "Created flag (enabled=${flag.enabled}$targeting)"
    }

    fun changes(before: FeatureFlag, after: FeatureFlag): String {
        val changes = buildList {
            if (before.name != after.name) add("name: '${before.name}' → '${after.name}'")
            if (before.description != after.description) {
                add("description: ${quote(before.description)} → ${quote(after.description)}")
            }
            if (before.enabled != after.enabled) add("enabled: ${before.enabled} → ${after.enabled}")
            if (before.targetingRule != after.targetingRule) {
                add("conditions: ${before.targetingRule.describe()} → ${after.targetingRule.describe()}")
            }
        }
        return if (changes.isEmpty()) "No changes" else changes.joinToString("; ")
    }

    fun toggle(before: FeatureFlag, after: FeatureFlag): String = "enabled: ${before.enabled} → ${after.enabled}"

    fun deletion(flag: FeatureFlag): String = "Deleted flag '${flag.key}'"

    private fun quote(value: String?): String = if (value == null) "∅" else "'$value'"
}
