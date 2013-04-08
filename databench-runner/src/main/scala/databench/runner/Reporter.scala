package databench.runner

import java.io.FileWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.io.PrintWriter
import databench.task.Generator

trait Reporter {
    def report(turn: TurnSummary): TurnSummary
}

class CSVReporter(val tasksGenerators: Seq[Generator]) extends Reporter {

    private val benchmarkDateTime =
        new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())

    private val writer =
        createWriter("benchmark-" + benchmarkDateTime + ".csv")

    private val failureLogWriter =
        createWriter("benchmark-" + benchmarkDateTime + "-failures.log")

    private val possibleTasksResultsNames =
        tasksGenerators.map(_.possibleResultsNames)
            .flatten.sortBy(name => name)

    writeHeader

    def report(turn: TurnSummary) = {
        val tasksResults = possibleTasksResultsNames.map(turn.numberOfResultsByName.getOrElse(_, 0))
        val line = Seq(turn.bankName, turn.vms, turn.threadsPerVM, turn.tps, turn.failures) ++ tasksResults
        write(writer)(line: _*)
        writer.flush
        writeFailuresLog(turn)
        turn
    }

    private def writeFailuresLog(turn: TurnSummary) = {
        val grouped = turn.exceptions.groupBy(ex => Option(ex.getMessage).getOrElse("NO MESSAGE"))
        for ((message, group) <- grouped) {
            write(failureLogWriter)(s"${turn.bankName}: ${group.size} exceptions with message $message")
            group.head.printStackTrace(new PrintWriter(failureLogWriter))
        }
        failureLogWriter.flush()
    }

    private def writeHeader = {
        val header = Seq("Subject", "VMs", "Threads/VM", "TPS", "Failures") ++ possibleTasksResultsNames
        write(writer)(header: _*)
        writer.flush
    }

    private def write(writer: FileWriter)(cells: Any*) =
        writer.write(cells.map(_.toString).mkString(";") + "\n")

    private def createResultsFolderIfNecessary = {
        val folder = new File("results")
        if (!folder.exists)
            folder.mkdir
    }

    private def createWriter(name: String) =
        new FileWriter(createFile(name))

    private def createFile(name: String) = {
        createResultsFolderIfNecessary
        val file =
            new File("results/" + name)
        require(file.createNewFile)
        file
    }

}