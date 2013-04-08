package databench.task

import scala.collection.immutable.Seq
import scala.util.Random

trait Generator extends Serializable {

    def possibleResultsNames: Set[String]

    def tasksFor(numberOfTasks: Int, numberOfAccounts: Int) =
        for (i <- 0 until numberOfTasks)
            yield taskFor(numberOfAccounts, i)

    protected def taskFor(numberOfAccounts: Int, index: Int): Task

    protected def randomAccountNumber(numberOfAccounts: Int) =
        Random.nextInt(numberOfAccounts)
}

object Generator {
    def genTasksFor(
        numberOfTasks: Int,
        numberOfAccounts: Int,
        generatorsPercentages: Seq[(Int, Generator)]) = {

        require(
            generatorsPercentages.map(_._1).sum == 100,
            "Invalid generator percentages.")
        val tasks = generatorsPercentages
            .map(gp => gp._2.tasksFor(numberOfTasks * gp._1 / 100, numberOfAccounts))
            .flatten
        Random.shuffle(tasks).to[Seq]
    }
}