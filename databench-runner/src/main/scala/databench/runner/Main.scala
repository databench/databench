package databench.runner

import databench.util.ClassPathLocator
import databench.Bank
import databench.properties
import databench.task.getAccountStatusGenerator
import databench.task.transferTaskGenerator
import scala.collection.immutable.Seq

object Main extends App {

    val subjects =
        ClassPathLocator
            .concreteImplementorsOf[Bank[Any]]
            .filter(_.getConstructors.find(_.getParameterTypes().isEmpty).isDefined)
            .map(new BankSubject(_))
            .sortBy(_.name)
            .filter(subject =>
                properties.filter.map(s =>
                    subject.name.toLowerCase.contains(s.toLowerCase))
                    .getOrElse(true))

    val generatorsPercentages = Seq(
        properties.readsPercent -> getAccountStatusGenerator,
        properties.writesPercent -> transferTaskGenerator)

    val reporter = new CSVReporter(
        generatorsPercentages.map(_._2))

    Benchmark(
        subjects,
        generatorsPercentages,
        numberOfTasks = properties.tasks,
        numberOfAccounts = properties.accounts,
        memory = properties.memory,
        reporter)
}