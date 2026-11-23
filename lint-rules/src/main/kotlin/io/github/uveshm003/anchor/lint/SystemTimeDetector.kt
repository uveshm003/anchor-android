package io.github.uveshm003.anchor.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.getParentOfType

/**
 * Flags production code that reads the system clock or the default time zone directly instead of the injected
 * `java.time.Clock`. Calls are resolved, so imports, aliases and fully qualified names are all caught.
 * [SystemZoneClock][ALLOWED_CLASSES] is the one class allowed to do it.
 */
class SystemTimeDetector :
    Detector(),
    SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = BANNED_BY_NAME.keys.toList()

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val owner = method.containingClass?.qualifiedName ?: return
        if (!BANNED_BY_NAME.getValue(method.name).invoke(owner, method) || node.isInAllowedClass()) return
        context.report(
            issue = ISSUE,
            scope = node,
            location = context.getLocation(node),
            message = "Read time from the injected `Clock`, not `${owner.substringAfterLast('.')}.${method.name}()`",
        )
    }

    private fun UElement.isInAllowedClass(): Boolean =
        generateSequence(getParentOfType<UClass>(UClass::class.java)) { it.getParentOfType(UClass::class.java) }
            .any { it.qualifiedName in ALLOWED_CLASSES }

    companion object {
        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "SystemTimeCall",
            briefDescription = "Direct system clock or time zone read",
            explanation = """
                Production code must get the time and zone from the injected `java.time.Clock` \
                (`SystemZoneClock` in the app, `TestClock` in tests), so time-based logic can be tested and follows \
                time-zone changes. Inject a `Clock` and use `clock.instant()`, `clock.millis()`, `clock.zone` or \
                `LocalDate.now(clock)`. `SystemZoneClock` is the only class allowed to read the system clock.
            """,
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(SystemTimeDetector::class.java, Scope.JAVA_FILE_SCOPE),
        )

        private val ALLOWED_CLASSES = setOf("io.github.uveshm003.anchor.core.common.time.SystemZoneClock")

        private val SYSTEM_CLOCK_FACTORIES = setOf(
            "system",
            "systemDefaultZone",
            "systemUTC",
            "tickMillis",
            "tickSeconds",
            "tickMinutes",
        )

        /** Method name to "is this owner's method a system time read". */
        private val BANNED_BY_NAME: Map<String, (String, PsiMethod) -> Boolean> = buildMap {
            put("currentTimeMillis") { owner, _ -> owner == "java.lang.System" }
            // Every java.time `now()` / `now(zone)`: only `now(clock)` takes time from a Clock.
            put("now") { owner, method -> owner.startsWith("java.time.") && !method.takesClock() }
            SYSTEM_CLOCK_FACTORIES.forEach { name -> put(name) { owner, _ -> owner == CLOCK_CLASS } }
            put("systemDefault") { owner, _ -> owner == "java.time.ZoneId" }
            put("getDefault") { owner, _ -> owner == "java.util.TimeZone" }
        }
    }
}

private const val CLOCK_CLASS = "java.time.Clock"

private fun PsiMethod.takesClock(): Boolean = parameterList.parameters.any { it.type.canonicalText == CLOCK_CLASS }
