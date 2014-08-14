# COMPILATION

To compile, enter the `src/` directory and execute the following command (in terminal):

`javac MySMTPServer.java`

Several class files should now have been generated in the directory.

# RUN

To run the SMTP server, ensure it has been compiled (see COMPILATION) and execute the
following command (in terminal):

`java MySMTPServer`

or, if you would like to fork to the background, execute (in terminal):

`java MySMTPServer &`

# TEST

To test the server, ensure the server is running (see RUN). Then, open up a new shell
prompt and telnet into the local server on port '6013' like so:

`telnet 127.0.0.1 6013`

You should now have established a connection with the SMTP server (you will see a 
`220` response).

You can now type in and use the following SMTP commands:

* `HELO` / `EHLO`
* `MAIL FROM` (note: domain must end in 'usyd.edu.au' or 'sydney.edu.au')
* `RCPT TO` (supports multiple recipients)
* `DATA`
* `NOOP`
* `RSET`
* `QUIT`

To check if an email has successfully been "sent", check the `src/emails` directory
(it should be automatically created if it doesn't already exist) and then check the
most recently modified text file.

To terminate the connection, please type in the command `QUIT`.
