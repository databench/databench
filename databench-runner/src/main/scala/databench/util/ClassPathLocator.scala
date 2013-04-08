package databench.util

import org.reflections.Reflections
import scala.collection.immutable.Seq
import java.lang.reflect.Modifier

object ClassPathLocator {

    private def getAllImplementorsNames(
        interfaceClass: Class[_]) = {
        val reflections = new Reflections("databench")
        val subtypes =
            reflections.getStore
                .getSubTypesOf(interfaceClass.getName)
                .toArray
        Seq(subtypes: _*).asInstanceOf[Seq[String]]
    }

    private def getAllImplementors(interfaceClass: Class[_]) =
        getAllImplementorsNames(interfaceClass)
            .map(Class.forName)

    def concreteImplementorsOf[T: Manifest] =
        getAllImplementors(manifest[T].runtimeClass)
            .to[Seq].asInstanceOf[Seq[Class[T]]]
            .filter(clazz => !Modifier.isAbstract(clazz.getModifiers))
}