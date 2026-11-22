ssignment2/VideoPlayerSingleThread
open -e src/main/java/comp3011/FrameProcessor.java 
(base) ritishanayyar@Ritishas-MacBook-Air VideoPlayerSingleThread % 
(base) ritishanayyar@Ritishas-MacBook-Air VideoPlayerSingleThread % cd ~/Documents/uni/year2/Sem2/CCP/Assignment2/COMP3011-Assignment2/VideoPlayerSingleThread
open -e src/main/java/comp3011/FrameProcessor.java
(base) ritishanayyar@Ritishas-MacBook-Air VideoPlayerSingleThread % mvn clean compile
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------< comp3011:VideoPlayerSingleThread >------------------
[INFO] Building VideoPlayerSingleThread0.1
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[WARNING] 6 problems were encountered while building the effective model for org.openjfx:javafx-graphics:jar:21 during dependency collection step for project (use -X to see details)
[INFO] 
[INFO] --- clean:3.2.0:clean (default-clean) @ VideoPlayerSingleThread ---
[INFO] Deleting /Users/ritishanayyar/Documents/uni/year2/Sem2/CCP/Assignment2/COMP3011-Assignment2/VideoPlayerSingleThread/target
[INFO] 
[INFO] --- resources:3.4.0:resources (default-resources) @ VideoPlayerSingleThread ---
[INFO] skip non existing resourceDirectory /Users/ritishanayyar/Documents/uni/year2/Sem2/CCP/Assignment2/COMP3011-Assignment2/VideoPlayerSingleThread/src/main/resources
[INFO] 
[INFO] --- compiler:3.15.0:compile (default-compile) @ VideoPlayerSingleThread ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 21 source files with javac [debug release 25] to target/classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  0.596 s
[INFO] Finished at: 2026-10-27T11:58:46+10:30
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.15.0:compile (default-compile) on project VideoPlayerSingleThread: Fatal error compiling: error: release version 25 notsupported -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoExecutionException
(base) ritishanayyar@Ritishas-MacBook-Air VideoPlayerSingleThread % 