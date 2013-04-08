package databench

import System.getProperty

object properties {

    private var myProperties = List[(String, Any)]()

    val tasks = int("tasks", 500000)
    val accounts = int("accounts", 50000)
    val memory = int("memory", 10000)

    val writesPercent = int("writesPercent", 20)
    val readsPercent = int("readsPercent", 100 - writesPercent)

    val vmsStart = int("vmsStart", 1)
    val vmsStep = int("vmsStep", 2)

    val threadsStart = int("threadsStart", 1)
    val threadsStep = int("threadsStep", 2)

    val postgreUser = string("postgreUser", "postgres")
    val postgrePassword = string("postgrePassword", "postgres")

    val filter = Option(getProperty("filter", null))

    myProperties ++= List(("filter", filter))

    override lazy val toString =
        "Properties:\n" + myProperties.map(tuple => "\t" + tuple._1 + "=" + tuple._2).mkString("\n")

    private def string(name: String, default: String) =
        prop[String](name, s => s, default)

    private def bool(name: String, default: Boolean) =
        prop[Boolean](name, _ == "true", default)

    private def int(name: String, default: Int) =
        prop[Int](name, Integer.parseInt(_), default)

    private def prop[T](name: String, function: String => T, default: T) = {
        val value =
            Option(getProperty(name))
                .map(function(_))
                .getOrElse(default)
        myProperties ++= List((name, value))
        value
    }

}