package databench.util

import grizzled.slf4j.{ Logging => GrizzledLogging }
import grizzled.slf4j.Logger

trait Logging {

    @transient
    val logger = Logger(getClass)

    protected def trace(s: String) =
        logger.trace(s)

    protected def debug(s: String) =
        logger.debug(s)

    protected def info(s: String) =
        logger.info(s)

    protected def warn(s: String) =
        logger.warn(s)

    protected def error(s: String) =
        logger.error(s)

    protected def logTrace[A](id: => String)(f: => A): A =
        logLevel(id, (s: String) => trace(s))(f)

    protected def logDebug[A](id: => String)(f: => A): A =
        logLevel(id, (s: String) => debug(s))(f)

    protected def logInfo[A](id: => String)(f: => A): A =
        logLevel(id, (s: String) => info(s))(f)

    protected def logWarn[A](id: => String)(f: => A): A =
        logLevel(id, (s: String) => warn(s))(f)

    protected def logError[A](id: => String)(f: => A): A =
        logLevel(id, (s: String) => error(s))(f)

    private[this] def logLevel[A](id: => String, level: (String) => Unit)(f: => A): A = {
        level(id)
        f
    }

}