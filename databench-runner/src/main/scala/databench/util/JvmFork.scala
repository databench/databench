package databench.util

import org.gfork.Fork
import java.io.File

case class FunctionTaskReturn(value: Any)
case class FunctionTask[R](f: () => R) {
    def run: Serializable =
        FunctionTaskReturn(f())
}

case class ForkTask[R](fork: Fork[FunctionTask[R], Nothing]) {
    def execute = {
        fork.execute
        this
    }
    def joinAndGetResult = {
        join
        getResult
    }
    def getResult = {
        val res = fork.getReturnValue.asInstanceOf[FunctionTaskReturn].value.asInstanceOf[R]
        clearTempFiles
        res
    }
    def join = {
        fork.waitFor
        println(fork.getStdOut)
        System.err.println(fork.getStdErr)
        Option(fork.getException).map(throw _)
    }
    private def clearTempFiles = {
        val fileFields =
            classOf[Fork[_, _]].getDeclaredFields
                .filter(_.getType == classOf[File])
        fileFields.foreach(_.setAccessible(true))
        val files =
            fileFields
                .map(_.get(fork).asInstanceOf[File])
                .filter(_ != null)
                .filter(_.isFile)
        files.foreach(_.delete)
    }
}

object JvmFork {

    Fork.setJvmOptionsForAll("-client")

    def fork[R](ms: Int = 100, mx: Int = 1024, others: List[String] = List())(f: => R): ForkTask[R] = {
        val fork = new Fork(FunctionTask(() => f), classOf[FunctionTask[_]].getMethod("run"))
        fork.addJvmOption("-Xmx" + mx + "M")
        fork.addJvmOption("-Xms" + ms + "M")
        others.map(fork.addJvmOption)
        ForkTask[R](fork)
    }

    def runForked[R](ms: Int = 100, mx: Int = 1024, others: List[String] = List())(f: => R): R =
        fork[R](ms, mx, others)(f).execute.joinAndGetResult
}