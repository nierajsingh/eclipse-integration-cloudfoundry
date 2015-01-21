# Cloud Foundry Integration for Eclipse

  
## Builds With Work In Progress

The following zip files contain installation sites with CF Eclipse builds that contain work in progress
that have not been merged upstream to the CF Eclipse repo.

To install, download the zip file in the link below, unzip in a temp directory. In STS, go to:

Help -> Install New Software -> Add

In the "Add" dialogue, select "Local" and browse to location where content was unzipped.

Click OK and then complete installation. STS/Eclipse would need to be restarted.


### CF 1.8.0 Debug Support - Overview

Currently, debugging an application in Pivotal Web Services (PWS) through CF Eclipse is only supported via ngrok.com.

Ngrok.com is used to establish a tunnel between the app running in the Cloud space and ngrok.com. In order to establish
this tunnel, the ngrok executable needs to be included in the application as well as a shell script to
run ngrok remotely in the Cloud space. Both need to be included as part of the application when pushing it to the Cloud space.

A prerequisite to debug on the Cloud space using ngrok.com is to have an ngrok.com account and authtoken provided by ngrok.com when you register. 

WARNING: Debugging through ngrok.com is NOT secure. It's meant as an experimental debugging implementation. Use at your own risk.

1. In your application's project in Eclipse, create a ".profile.d" folder in src/main/resources and make sure src/main/resources is in the project's classpath, if it isn't already. The presence of the ".profile.d" folder will enable the debug UI in CF Eclipse for that application.

2. In ".profile.d", add a Linux ngrok executable, which can be downloaded from here: [ngrok](https://ngrok.com/download)

3. Add a ngrok.sh in ".profile.d" file with the following content: [ngrok.sh](ngrok.sh)

4. Push the application to your Cloud space using drag/drop or WTP Run on Server

5. Once the application has been pushed, double-click it in the Eclipse Servers view to open the CF Editor, and the "Debug" button should be enabled. The application can now be connected to the debugger by clicking "Debug" without restarting the application. It can also be disconnected at any time without stopping the application.

#### Using another port instead of 4000

By default, CF Eclipse tells ngrok.com to use 4000 to connect to the JVM running in the Cloud space, which CF Eclipse automatically sets as part of JAVA_OPTS environment variable when you click "Debug". If another port is to be used, it needs to be specified in both JAVA_OPTS and ngrok.sh MANUALLY.

Example:

If using 4205 instead of 4000, set or edit the JAVA_OPTS application environment variable manually through the Cloud Foundry editor (double-click on the published application in the Servers view to open the editor). Set the following value for JAVA_OPTS:

-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4205,suspend=n

then ngrok.sh should be:

/app/.profile.d/ngrok -proto=tcp -authtoken [your-ngrok-authtoken-here] -log=stdout 4205 > /app/.profile.d/ngrok.txt &

If the application was already pushed and running when these changes are made, your application needs to be stopped and started in order for the changes to take effect. 

If setting the environment variable while the application is being pushed for the first time in the Application Deployment Wizard, then it is NOT necessary to stop and start the application. Stopping and starting the application only needs to happen if JAVA_OPTS and ngrok.sh are changed manually WHILE the application is running.

If you want to use the default port, the manual steps above are not needed as CF Eclipse will automatically set JAVA_OPTS for you when "Debug" is pressed.


### CF 1.8.0 Debug Support - Downloads

[v2 - 20150121 - Improved Debug Features](cfeclipse180debug_20150121.zip)

- Debug button now enabled for any Java app that has a ".profile.d" folder in classpath in the app's workspace project. Apps
that do not contain a ".profile.d" will not have Debug enabled

- Debug is enabled for the application both when it is in stopped and start state. Applications
no longer needs to be pushed in stopped state in order to enable debug.

- If the application is running, and being debugged for the first time, JAVA_OPTS env variable
will be set automatically by CF Eclipse, unless it already exists with "-Xdebug" (see below), and prompt user whether to continue with app restart. If user clicks "Yes", CF Eclipse will automatically restart the application and then connect to the debugger. The prompt only occurs if CF Eclipse detects that env var was not set. If it was already set from a previous start, or set externally, it will not prompt to restart the application.

- If application is stopped, and being debugged for the first time, CF Eclipse will add any env vars
automatically without prompting the user and automatically start the app, and connect to debugger.

- Once an application is successfully connected to the debugger, the "Debug" button in the editor changes
from to "Disconnect". Users can connect and disconnect as many times as they want while the application is
running without having to restart the application.


