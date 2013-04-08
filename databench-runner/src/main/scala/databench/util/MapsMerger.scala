package databench.util

object MapsMerger {

    def mergeMaps(maps: Map[String, Float]*): Map[String, Float] =
        maps.foldLeft(Map[String, Float]())(mergeMaps)

    private def mergeMaps(mapA: Map[String, Float], mapB: Map[String, Float]): Map[String, Float] =
        mapA ++ (for ((k, v) <- mapB) yield (k -> (v + mapA.getOrElse(k, 0f))))
}