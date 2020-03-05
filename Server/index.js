//https://socket.io/get-started/chat/
//const _ = require("lodash");
const port = 3000;
const app = require('express')();
const http = require('http').createServer(app);
const io = require('socket.io')(http);

//const app = express();

app.get('/', function(req, res) {
   res.sendFile(__dirname + '/html/index.html');
});

io.on('connection', function(socket){
   console.log('a user connected');

   socket.on('chat message', function(msg){
      console.log('message: ' + msg);
      io.emit('chat message', msg);
   });

   socket.on('disconnect', function(){
      console.log('user disconnected');
   });
});

http.listen(port, function() {
  console.log('listening on *:' + port);
});
// Used to verify login info is correct
/*
app.post("/login", async function(req, res) {
  if (!req.body) {
    res.status(401).json({
      message: "No req.body present"
    });
  } else if (req.body.name && req.body.password) {
    try {
      // checks the database and then determines if the passwords matchs
      console.log("user login attempt");
      let user = await FIREBASE.getUser(req.body.name);

      if (!user) {
        res.status(401).json({
          message: "username not found"
        });
        console.log("username not found");
      }

      if (user.password === req.body.password) {
        res.json({
          message: "ok"
        });
        console.log("ok");
      } else {
        res.status(401).json({
          message: "passwords do not match"
        });
        console.log("passwords do not match");
      }
    } catch (err) {
      res.status(401).json({
        message: "Login Error"
      });
      console.log("Login Error");
    }
  }
});
*/

