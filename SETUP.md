# Setup

## databench-sqltyped

Sqltyped needs a connection to database already at compile time. To setup database run:

$ sudo -u postgres createdb -O postgres databenchdev
$ sudo -u postgres psql databenchdev < databench/databench-sqltyped/src/main/resources/schema.sql

Then if you use Eclipse to compile the project add these to your 'eclipse.ini':

```
-Dsqltyped.url=jdbc:postgresql://localhost/databenchdev
-Dsqltyped.driver=org.postgresql.Driver 
-Dsqltyped.username=postgres 
-Dsqltyped.password=postgres
```

If Ensime/Emacs is used pass configuration in ENSIME_JVM_ARGS environment variable:

```
ENSIME_JVM_ARGS="-Dsqltyped.url=jdbc:mysql://localhost:3306/mydb -Dsqltyped.driver=com.mysql.jdbc.Driver -Dsqltyped.username=root -Dsqltyped.password=" emacs
```
