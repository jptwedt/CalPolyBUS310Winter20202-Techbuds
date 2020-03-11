//https://socket.io/get-started/chat/
const port = 3000;
const express = require('express');
const http = require('http');
const app = express();
const server = http.createServer(app);
const io = require('socket.io').listen(server);
const RATE = 70.0;
var startTime;
var endTime;
var clientlat = 0.0;
var clientlong = 0.0;
var techlat = 0.0;
var techlong = 0.0;
var distance = 100.0;
var jobnotify = true;
var jobgoing = false;
var loccount = 0;
var moneycount = 0;
var travelnotify = true;

app.get('/', function(req, res) {
   res.sendFile(__dirname + "/html/index.html");
});

io.on('connection', function(socket){

   console.log('a user connected');

   socket.on('techLocation', function(msg){
      var semi = msg.search(';');
      techlat = msg.substr(0, semi);
      techlong = msg.substr(semi + 1, msg.length);
      distance = getDistance();
      var techpos = {
         techlat: techlat,
         techlong: techlong
      }
      io.emit('servertechpos', techpos);
      console.log('techLocation: ' + msg);
      if(travelnotify){
         loccount++;
         if(loccount % 2 == 0){
            io.emit("statusUpdate", "Technician en route");
         }
      }
      if(jobnotify && hasArrived()){
         io.emit("techArrived", "Technician Arrived");
      }
      if(jobgoing){
         moneycount++;
         if(moneycount % 2 == 0){
            io.emit("statusUpdate","Making $$$!");
         }
      }

   });

   socket.on('clientLocation', function(msg){
      var semi = msg.search(';');
      clientlat = msg.substr(0, semi);
      clientlong = msg.substr(semi + 1, msg.length);
      var clientpos = {
         clientlat: clientlat,
         clientlong: clientlong
      }
      io.emit('serverclientpos', clientpos);
      console.log('clientLocation: ' + msg);

      if(jobnotify && hasArrived()){
         io.emit("statusUpdate", "Technician Arrived");
      }
   });

   socket.on('jobStart', function(msg){
      jobgoing = true;
      startTime = Date.now();
      console.log(msg);
      io.emit("statusUpdate","Job started at: " + startTime);
      io.emit("statusUpdate","Making $$$!");
   });

   socket.on('jobEnd', function(msg){
      jobgoing = false;
      endTime = Date.now();
      let ttlTime = (endTime - startTime) / 1000;
      let moneyOwed = RATE / 60 / 60 * ttlTime;
      console.log(msg);
      io.emit("statusUpdate","Job ended at: " + endTime);
      io.emit("statusUpdate","Job time: " + ttlTime + " seconds");
      io.emit("statusUpdate","Total made: $" + moneyOwed);
   });

   socket.on('clientDebug', function(msg){
      console.log("client status: " + msg);
   });

   socket.on('techDebug', function(msg){
      console.log("tech status: " + msg);
   });

   socket.on('disconnect', function(){
      console.log('user disconnected');
      jobnotify = true;
      jobgoing = false;
      loccount = 0;
      moneycount = 0;
      travelnotify = true;
   });
});

function hasArrived(){
   distance = getDistance();
   if(distance < 0.00005){
      console.log("technician arrived: " + distance);
      jobnotify = false;
      travelnotify = false;
      return true;
   }
   return false;
}

function getDistance(){
   return Math.sqrt(Math.pow(clientlat - techlat, 2.0) 
      + Math.pow(clientlong -techlong, 2.0));
}

server.listen(port, function() {
  console.log('Node app running on *:' + port);
});

