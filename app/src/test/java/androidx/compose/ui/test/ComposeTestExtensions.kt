@file:JvmName("ComposeTestExtensionsKt")
package androidx.compose.ui.test

/**
 * Top-level extension shims for SemanticsNodeInteraction.assertExists() and assertDoesNotExist().
 * In compose-ui-test 1.7.x and 1.8.x, these are member functions on SemanticsNodeInteraction,
 * not top-level extension functions. The test file imports them as top-level functions,
 * so we provide these shims to bridge the gap.
 */

fun assertExists(node: SemanticsNodeInteraction): SemanticsNodeInteraction =
    node.assertExists()

fun assertDoesNotExist(node: SemanticsNodeInteraction) =
    node.assertDoesNotExist()
