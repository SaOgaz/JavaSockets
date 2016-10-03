Notes so far:

On my work machine I had to use a port above 1024 since I don't have permission on my account to use lower port numbers.

I also just used a "moka.local" (moka is the name of my machine) for the server listing, here's the calls I used to get this running:

In terminal 1:
javac EchoServer.java
java -cp . EchoServer 1025

In ternminal 2:
javac EchoClient.java
java -cp . EchoClient moka.local 1025

Here's the page I copied this program from.. with a few tiny tweaks: http://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html.  We can ignore hte JavaSockets.java program, it's empty.
