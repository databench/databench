package databench.runner

import language.existentials
import databench.Bank
import databench.SingleVMBank

case class BankSubject(clazz: Class[_ <: Bank[_]], instanceOption: Option[Bank[Any]] = None) {

    def this(instance: Bank[_]) =
        this(instance.getClass, Some(instance.asInstanceOf[Bank[Any]]))

    val name =
        clazz.getSimpleName()

    val acceptMultipleVMs =
        !classOf[SingleVMBank].isAssignableFrom(clazz)

    @transient lazy val instance =
        instanceOption.getOrElse(clazz.newInstance.asInstanceOf[Bank[Any]])

    override def toString = name
}