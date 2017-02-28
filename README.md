README
################
systems to be used to run the program :
dc01.utdallas.edu
dc02.utdallas.edu
dc03.utdallas.edu
dc04.utdallas.edu
dc05.utdallas.edu
dc06.utdallas.edu
dc07.utdallas.edu
dc08.utdallas.edu
dc09.utdallas.edu
dc10.utdallas.edu

To run the program "MutEx.java", we need to compile it in any one of the above systems. 
This program needs the text file "nodeList.txt" containing the node number, domain name and port numbers. The format and content of the file should be as follows. 

0:dc01.utdallas.edu:6400
1:dc02.utdallas.edu:6401
2:dc03.utdallas.edu:6402
3:dc04.utdallas.edu:6403
4:dc05.utdallas.edu:6404
5:dc06.utdallas.edu:6405
6:dc07.utdallas.edu:6406
7:dc08.utdallas.edu:6407
8:dc09.utdallas.edu:6408
9:dc10.utdallas.edu:6409

dc01.utdallas.edu -> has to be the node 0. 
To run the "MutEx.class" file, "nodeList.txt" should be within the same folder as from where the execution will be initiated.

Follow the following sequence to execute:
1. ssh to all the system mentioned above.
2. compile "MutEx.java" in any one of the above systems.
3. execute:
	java MutEx 0 in dc01.utdallas.edu’s terminal
	java MutEx 1 in dc02.utdallas.edu’s terminal
	java MutEx 2 in dc03.utdallas.edu’s terminal
	java MutEx 3 in dc04.utdallas.edu’s terminal
	java MutEx 4 in dc05.utdallas.edu’s terminal
	java MutEx 5 in dc06.utdallas.edu’s terminal
	java MutEx 6 in dc07.utdallas.edu’s terminal
	java MutEx 7 in dc08.utdallas.edu’s terminal
	java MutEx 8 in dc09.utdallas.edu’s terminal
	java MutEx 9 in dc10.utdallas.edu’s terminal

The program executes and terminates. The output will be written in to "CriticalSection.txt" file in the following format as soon as a node completes critical section execution:

Critical Section = <critical Section number> ||| Entering node: <node number> : @ local clock value = <clock value> ||| messages exchanged <# msgs exchanged> ||| Time elapsed in milliseconds = <time taken to execute containing Section>

the total number of messages sent and received by a node will be displayed in the terminals of corresponding nodes as follows:
Total number of requests sent = 80
Total number of replies sent = 80
Total number of requests received = 80
Total number of replies received = 80

