//https://socket.io/get-started/chat/
//const _ = require("lodash");
const port = 3000;
const express = require('express');
const http = require('http');
const app = express();
const server = http.createServer(app);
const io = require('socket.io').listen(server);

//const app = express();

app.get('/', function(req, res) {
   res.send("running on " + port);
});

io.on('connection', function(socket){

   console.log('a user connected');

   socket.on('techLocation', function(msg){
      console.log('techLocation: ' + msg);
      io.emit('techLocation', msg);
   });

   socket.on('clientLocation', function(msg){
      console.log('clientLocation: ' + msg);
      io.emit('clientLocation', msg);
   });

   socket.on('disconnect', function(){
      console.log('user disconnected');
   });
});

server.listen(port, function() {
  console.log('Node app running on *:' + port);
});

