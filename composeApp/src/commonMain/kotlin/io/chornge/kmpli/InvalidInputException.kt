package io.chornge.kmpli

/**
 * Signals that the user supplied something kmpli cannot accept: a malformed project name, an
 * unknown template, a bad platform spec.
 *
 * These are the CLI's normal contract being exercised, not defects, so they are reported as a
 * message and a non-zero exit rather than as a crash. Programmer errors and unexpected IO
 * failures keep using other exception types, so a caller, or a test, can always tell the two
 * apart.
 */
class InvalidInputException(message: String) : Exception(message)

/**
 * Throws [InvalidInputException] with [message].
 *
 * Mirrors `kotlin.error` so validation call sites read the same way, while signalling a user
 * mistake rather than a violated internal invariant.
 */
internal fun invalidInput(message: String): Nothing = throw InvalidInputException(message)
