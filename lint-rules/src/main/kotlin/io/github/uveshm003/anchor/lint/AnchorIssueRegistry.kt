package io.github.uveshm003.anchor.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

/** Anchor's own lint checks, loaded by every module through `lintChecks(project(":lint-rules"))`. */
class AnchorIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> = listOf(SystemTimeDetector.ISSUE)

    override val api: Int = CURRENT_API

    override val vendor: Vendor = Vendor(
        vendorName = "Anchor",
        identifier = "anchor-lint-rules",
        feedbackUrl = "https://github.com/uveshm003/anchor-android/issues",
    )
}
