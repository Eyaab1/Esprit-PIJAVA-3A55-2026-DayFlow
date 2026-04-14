$env:JAVA_HOME = "C:\Users\Mariem&Islem\.jdks\corretto-23.0.2"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\plugins\maven\lib\maven3\bin\mvn.cmd"
& $mvn clean javafx:run -f pom.xml
