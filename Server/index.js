var express = require('express');
var app = express();
const PORT = 3000;

var server = app.listen(PORT, function(){
   console.log("server started on port " + PORT);
});

app.get('/', function(req, res){
   res.send('Hello world');
});
