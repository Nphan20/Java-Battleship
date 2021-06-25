# Battleship!

In the listed files, I've made a Battleship program for a final project in my Java Programming 2 College course, and used the JFrame library and the use of ServerSocket programming.

You first initialize the server, which listens for two players, or in this case, two Sockets.

When the server is started, the user can then run the GUI program, which acts as both the client and the server.

The way it begins, each player can set down their ships on their grids, and it'll save.

When both players are ready, it'll allow the server, player 1, go first. They pick a location on the board on the right of their screen. IF they manage to land a hit, they change
the ship color to RED, which means that it was hit on the enemy side (in this case, player 2).

The turns will switch back and forth until the ships are cleared. Unfortunately, I haven't implemented the feature to end the game yet.
