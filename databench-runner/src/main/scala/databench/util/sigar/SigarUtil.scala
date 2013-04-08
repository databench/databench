package databench.util.sigar

import databench.util.MapsMerger._
import language.implicitConversions
import org.hyperic.sigar.Sigar
import org.hyperic.sigar.ptql.ProcessFinder
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import databench.database.Database
import java.io.File
import org.hyperic.sigar.SigarException
import databench.util.Logging
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.io.FileOutputStream
import scala.collection.immutable.Seq

object SigarLib {

    def getSigar = {
        load
        new Sigar
    }

    def unzip(stream: InputStream, folder: String) = {
        val in = new ZipInputStream(stream);
        var entryOption = Option(in.getNextEntry())
        while (entryOption.isDefined) {
            val entry = entryOption.get;
            val outFilename = entry.getName();
            val out = new FileOutputStream(folder + outFilename);
            val buffer = new Array[Byte](1024)
            Iterator.continually(in.read(buffer))
                .takeWhile(_ != -1)
                .foreach { out.write(buffer, 0, _) }
            out.close()
            entryOption = Option(in.getNextEntry())
        }
    }

    lazy val load = {
        val tmpDir = System.getProperty("java.io.tmpdir") + "sigarlib/"
        new File(tmpDir).mkdir()
        unzip(getClass.getResourceAsStream("sigarlib.zip"), tmpDir);
        new File(tmpDir).listFiles.foreach(_.setExecutable(true))
        System.setProperty("org.hyperic.sigar.path", tmpDir)
    }

}

class SystemMonitor {

    val processesToDetail =
        Database.instances.map(_.processName).flatten

    val sigar = SigarLib.getSigar

    private var stopFlag = false

    private val monitors =
        List[SigarMonitor](OSMonitor(sigar), new ProcessMonitor("vm", List(sigar.getPid), sigar)) ++
            processesToDetail.map(new ProcessMonitor(_, sigar)).filter(_.enabled)

    private val snapshots = ListBuffer[Map[String, Float]]()

    private val thread = new Thread {
        override def run =
            while (!stopFlag) {
                snapshots += mergeMaps(monitors.map(_.takeSnapshot): _*)
                Thread.sleep(10)
            }
    }

    thread.start

    def stopAndGetSnapshots = {
        stopFlag = true
        thread.join
        snapshots.to[Seq]
    }
}

trait SigarMonitor {
    def takeSnapshot: Map[String, Float]
    val name: String
    val monitorKeysMap = Map(
        "Idle" -> ("cpu", "idle"),
        "User" -> ("cpu", "user"),
        "Sys" -> ("cpu", "sys"),
        "Wait" -> ("cpu", "wait"),
        "Free" -> ("mem", "free"),
        "Used" -> ("mem", "used"),
        "Resident" -> ("mem", "resident"),
        "PageFaults" -> ("mem", "page-faults"),
        "Size" -> ("mem", "size"))

    protected implicit def cast(map: java.util.Map[_, _]) =
        map.asInstanceOf[java.util.Map[String, String]].collect {
            case (key: String, value: String) if (monitorKeysMap.contains(key)) =>
                val (prefix, postfix) = monitorKeysMap(key)
                prefix + "-" + name + "-" + postfix -> java.lang.Float.parseFloat(value)
        }.toMap

}

case class OSMonitor(sigar: Sigar) extends SigarMonitor {

    val name = "os"

    def takeSnapshot =
        mergeMaps(
            sigar.getCpu.toMap,
            sigar.getMem.toMap)
}

class ProcessMonitor(processName: String, pids: => List[Long], sigar: Sigar) extends SigarMonitor with Logging {

    def this(processName: String, sigar: Sigar) =
        this(processName, new ProcessFinder(sigar).find("State.Name.eq=" + processName).toList, sigar)

    val name = processName

    val enabled = {
        try {
            takeSnapshot
            true
        } catch {
            case e: SigarException if (e.getMessage == "Operation not permitted") =>
                warn("User hasn't permission to monitor processes from another users, " +
                    "run databench as root to monitor databases processes.")
                false
        }
    }

    def takeSnapshot = {
        val mems = pids.map(pid => cast(sigar.getProcMem(pid).toMap))
        val cpus = pids.map(pid => cast(sigar.getProcCpu(pid).toMap))
        mergeMaps(mems ++ cpus: _*)
    }

}

