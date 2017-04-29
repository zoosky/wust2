package wust.backend

import com.typesafe.config.{Config => TConfig, ConfigFactory}

object ConfigExt {
  implicit class RichConfig(val conf: TConfig) extends AnyVal {
    def getOption[T](path: String, getter: TConfig => String => T): Option[T] = conf.hasPath(path) match {
      case true => Some(getter(conf)(path))
      case false => None
    }
  }
}

object Config {
  import ConfigExt._
  private val wustConfig = ConfigFactory.load.getConfig("wust")

  object auth {
    private val config = wustConfig.getConfig("auth")

    val enableImplicit: Boolean = config.getBoolean("enableImplicit")
    val tokenLifetime: Long = config.getLong("tokenLifetimeSeconds")
    val secret: String = config.getString("secret")
  }

  object email {
    private val config = wustConfig.getConfig("email")
    private val smtpConfig = config.getConfig("smtp")

    val fromAddress: Option[String] = config.getOption("fromAddress", _.getString)
    val smtp = for {
      smtpUsername <-smtpConfig.getOption("username", _.getString)
      smtpPassword <- smtpConfig.getOption("password", _.getString)
      smtpEndpoint <- smtpConfig.getOption("endpoint", _.getString)
    } yield new {
      val username = smtpUsername
      val password = smtpPassword
      val endpoint = smtpEndpoint
    }
  }
}
