# Distributed Shared Whiteboard Report

## Overview
This project implements a Java Swing distributed shared whiteboard using TCP sockets. The first user starts the server through `CreateWhiteBoard` and becomes the manager. Other users connect through `JoinWhiteBoard`, request approval from the manager, and receive the current drawing state after approval.

## Implemented Features
- Multi-user shared canvas with free draw, eraser, line, rectangle, oval, triangle, text, 16 colours, and selectable stroke size.
- Central server stores authoritative drawing state and broadcasts drawing events to all accepted clients.
- Manager approval for joins, duplicate username rejection, online user list, peer leave, and manager kick.
- Chat window with chat history replay for users who join later.
- Manager-only file menu: New, Open, Save, Save As, Export PNG, and Close.
- Text-based whiteboard save/load format with header `SHARED_WHITEBOARD_TEXT_V1`.
- Portable PNG export for viewing the current board outside the application.
- Manager shutdown notifies all peers and closes the server cleanly.

## How To Run
Compile:

```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java -Path src).FullName
```

Start the manager:

```powershell
java -cp out CreateWhiteBoard 127.0.0.1 5000 manager
```

Join as a peer:

```powershell
java -cp out JoinWhiteBoard 127.0.0.1 5000 user1
```

Run the automated persistence test:

```powershell
java -cp out test.WhiteboardFileCodecTest
```

## Design Notes
The server is the single source of truth for board state, users, and chat history. Clients send completed drawing elements to the server, and the server rebroadcasts them. New clients receive the stored drawing state and chat history after joining.

Save/Open uses a simple UTF-8 text format for drawing objects. Export PNG is intentionally separate because PNG is portable for viewing but cannot preserve editable drawing objects.

## Remaining Limitations
- No automated GUI or socket integration tests.
- Chat history is kept in memory only and is not saved in whiteboard files.
- Saved whiteboard files are portable text files for this assignment app, not a general-purpose graphics standard.
