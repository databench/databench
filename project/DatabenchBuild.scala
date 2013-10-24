import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object DatabenchBuild extends Build {

	lazy val databench = 
    	Project(
    		id = "databench",
    		base = file("."),
    		aggregate = Seq(databenchBank, databenchActivate,
    			databenchSlick, databenchPrevayler, databenchJpa,
    			databenchSqueryl, databenchDb4o, databenchRunner,
    			databenchChronicle, databenchSorm, databenchMapperDao)
    	)

    val postgresql = "postgresql" % "postgresql" % "9.1-901.jdbc4"
    val mongoDriver = "org.mongodb" % "mongo-java-driver" % "2.11.3"
    val scalaCompiler = "org.scala-lang" % "scala-compiler" % "2.10.2"
    val boneCP = "com.jolbox" % "bonecp" % "0.8.0-rc3"

    lazy val databenchBank = 
		Project(
			id = "databench-bank",
			base = file("databench-bank"),
    		settings = commonSettings ++ Seq(
		      libraryDependencies ++= 
		    	  Seq(postgresql, mongoDriver, scalaCompiler, boneCP)
    	  	)
		)

	val activateVersion = "1.4.4"
	val activateCore = "net.fwbrasil" %% "activate-core" % activateVersion exclude("org.ow2.asm", "asm")
	val activatePrevayler = "net.fwbrasil" %% "activate-prevayler" % activateVersion exclude("xpp3", "xpp3_min") exclude("org.ow2.asm", "asm")
	val activateJdbc = "net.fwbrasil" %% "activate-jdbc" % activateVersion exclude("org.ow2.asm", "asm")
	val activateMongo = "net.fwbrasil" %% "activate-mongo" % activateVersion exclude("org.ow2.asm", "asm")
	val activatePrevalent = "net.fwbrasil" %% "activate-prevalent" % activateVersion exclude("org.ow2.asm", "asm")

 	lazy val databenchActivate = 
		Project(
			id = "databench-activate",
			base = file("databench-activate"),
			dependencies = Seq(databenchBank),
			settings = commonSettings ++ Seq(
		      libraryDependencies ++= 
		    	  Seq(activateCore, activatePrevayler, activatePrevalent,
		    	  	activateJdbc, activateMongo, boneCP)
		    )
		)

	val slick = "com.typesafe.slick" %% "slick" % "1.0.1"

	lazy val databenchSlick = 
		Project(
			id = "databench-slick",
			base = file("databench-slick"),
			dependencies = Seq(databenchBank),
			settings = commonSettings ++ Seq(
		      libraryDependencies ++= 
		    	  Seq(slick, boneCP)
		    )
		)

	val sqltyped = "fi.reaktor" %% "sqltyped" % "0.3.0"

	lazy val databenchSqltyped = 
		Project(
			id = "databench-sqltyped",
			base = file("databench-sqltyped"),
			dependencies = Seq(databenchBank),
			settings = commonSettings ++ Seq(
		      libraryDependencies ++= 
		    	  Seq(sqltyped, slick, boneCP),
                          initialize ~= { _ => initSqltyped }
		    )
		)

        def initSqltyped {
          System.setProperty("sqltyped.url", "jdbc:postgresql://localhost/databenchdev")
          System.setProperty("sqltyped.driver", "org.postgresql.Driver")
          System.setProperty("sqltyped.username", "postgres")
          System.setProperty("sqltyped.password", "postgres")
        }

	val prevaylerCore = "org.prevayler" % "prevayler-core" % "2.6"
	val prevaylerFactory = "org.prevayler" % "prevayler-factory" % "2.6"
	val prevaylerXStream = "org.prevayler.extras" % "prevayler-xstream" % "2.6"

	lazy val databenchPrevayler = 
		Project(
			id = "databench-prevayler",
			base = file("databench-prevayler"),
			dependencies = Seq(databenchBank),
			settings = commonSettings ++ Seq(
		      libraryDependencies ++= 
		    	  Seq(prevaylerCore, prevaylerFactory, 
		    	  	prevaylerXStream)
		    )
		)

  val chronicleCore = "com.higherfrequencytrading" % "chronicle" % "1.8.2"
  val trove4j = "net.sf.trove4j" % "trove4j" % "3.0.3"
  val junit = "junit" % "junit" % "4.4" % "test"

  lazy val databenchChronicle =
    Project(
      id = "databench-chronicle",
      base = file("databench-chronicle"),
      dependencies = Seq(databenchBank),
      settings = commonSettings ++ Seq(
        libraryDependencies ++=
          Seq(chronicleCore, trove4j, junit)
      )
    )

	val hibernateEntityManager = "org.hibernate" % "hibernate-entitymanager" % "4.2.5.Final"
	val eclipselink = "org.eclipse.persistence" % "eclipselink" % "2.5.1"
	val batoo = "org.batoo.jpa" % "batoo-jpa" % "2.0.1.2"
	val validation = "javax.validation" % "validation-api" % "1.0.0.GA"
	val catalina = "org.apache.tomcat" % "catalina" % "6.0.14"
	
	lazy val databenchJpa = 
		Project(
			id = "databench-jpa",
			base = file("databench-jpa"),
			dependencies = Seq(databenchBank),
			settings = commonSettings ++ Seq(
		      libraryDependencies ++= 
		    	  Seq(hibernateEntityManager, catalina, eclipselink, batoo, validation, boneCP)
		    )
		)

	val squeryl = "org.squeryl" %% "squeryl" % "0.9.5-6"
	
	lazy val databenchSqueryl = 
		Project(
			id = "databench-squeryl",
			base = file("databench-squeryl"),
			dependencies = Seq(databenchBank),
			settings = commonSettings ++ Seq(
		      libraryDependencies ++= 
		    	  Seq(squeryl, boneCP)
		    )
		)

	val db4o = "com.db4o" % "db4o-all-java5" % "8.0.249.16098"

	lazy val databenchDb4o = 
		Project(
			id = "databench-db4o",
			base = file("databench-db4o"),
			dependencies = Seq(databenchBank),
			settings = commonSettings ++ Seq(
		      libraryDependencies ++= 
		    	  Seq(db4o)
		    )
		)

	val ebean = "org.avaje" % "ebean" % "2.8.1"

	lazy val databenchEbean = 
		Project(
			id = "databench-ebean",
			base = file("databench-ebean"),
			dependencies = Seq(databenchBank),
			settings = commonSettings ++ Seq(
		      libraryDependencies ++= 
		    	  Seq(ebean)
		    )
		)

	lazy val databenchJdbc = 
		Project(
			id = "databench-jdbc",
			base = file("databench-jdbc"),
			dependencies = Seq(databenchBank),
			settings = commonSettings ++ Seq(
		      libraryDependencies ++= 
		    	  Seq(postgresql, boneCP)
		    )
		)

	val sorm = "org.sorm-framework" % "sorm" % "0.3.9"

	lazy val databenchSorm = 
		Project(
			id = "databench-sorm",
			base = file("databench-sorm"),
			dependencies = Seq(databenchBank),
			settings = commonSettings ++ Seq(
		      libraryDependencies ++= 
		    	  Seq(sorm)
		    )
		)

	val mapperDao = "com.googlecode.mapperdao" % "mapperdao" % "1.0.0.rc24-2.10.2"

	lazy val databenchMapperDao =
		Project(
			id = "databench-mapperdao",
			base = file("databench-mapperdao"),
			dependencies = Seq(databenchBank),
			settings = commonSettings ++ Seq(
		      libraryDependencies ++= 
		    	  Seq(mapperDao, boneCP)
		    )
		)


	val reflections = "org.reflections" % "reflections" % "0.9.8" exclude("javassist", "javassist") exclude("dom4j", "dom4j")
	val gfork = "org.gfork" % "gfork" % "0.11"
	val scalaTest = "org.scalatest" %% "scalatest" % "2.0.M5b" % "test"

	lazy val databenchRunner =
		Project(
			id = "databench-runner",
			base = file("databench-runner"),
			dependencies = Seq(databenchBank, databenchActivate,
		    			databenchSlick, databenchPrevayler, databenchJpa,
		    			databenchSqueryl, databenchDb4o, databenchEbean, 
		    			databenchSqltyped, databenchJdbc, databenchChronicle,
		    			databenchSorm, databenchMapperDao),
			settings = commonSettings ++ assemblySettings ++ Seq(
					libraryDependencies ++= Seq(
						reflections, gfork, scalaTest),
	            	mainClass in assembly := Some("databench.runner.Main"),
	            	test in assembly := {},
			        mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
					  {
					    case PathList("META-INF", "services", xs) => MergeStrategy.concat
					    case other => MergeStrategy.last
					  }
					},
		            excludedFiles in assembly := { (base: Seq[File]) => (base / "META-INF" / "MANIFEST.MF").get }
            	)
			)

	val fwbrasilRepo = "fwbrasil.net" at "http://fwbrasil.net/maven/"
	val jbossRepo = "jboss" at "http://repository.jboss.org/maven2"
	val db4oRepo = "db4oRepo" at "http://source.db4o.com/maven"
	val jbossRepoThirdParty = "jbossRepoThirdParty" at "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases"
	val localMaven = "Local Maven Repository" at "file://"+Path.userHome+"/.m2/repository"
	val eclipselinkRepo = "eclipselink" at "http://download.eclipse.org/rt/eclipselink/maven.repo"
	val sonatypeSnapshots = "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

	def commonSettings = 
    	Defaults.defaultSettings ++ net.virtualvoid.sbt.graph.Plugin.graphSettings ++ Seq(
    		organization := "databench",
    		version := "1.0-SNAPSHOT",
    		scalaVersion := "2.10.2",
    		javacOptions ++= Seq("-source", "1.5", "-target", "1.5"),
    	    resolvers ++= Seq(fwbrasilRepo, jbossRepo, jbossRepoThirdParty, localMaven, eclipselinkRepo, sonatypeSnapshots),
    	    compileOrder := CompileOrder.ScalaThenJava,
    	    parallelExecution in Test := false
    	)

}
