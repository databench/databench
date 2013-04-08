package databench.task

import scala.collection.immutable.Seq
import databench.Bank
import databench.runner.ThreadTasks

trait Task {

    def name = getClass.getSimpleName.split('$').last
    def perform(bank: Bank[Any], idsMap: Map[Int, Any]): TaskResponse
}

trait TaskResponse {

    def resultFor(tasksGroups: Seq[ThreadTasks]): TaskResult
}

trait TaskResult {

    def name: String
}

