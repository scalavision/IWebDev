var server = require('net').createServer();
//var postcss = require('gulp-postcss');
var print = require('gulp-print');
var gulp = require('gulp');
var autoprefixer = require('autoprefixer');
var gutil = require('gulp-util');
var map = require('map-stream');
var vfs = require('vinyl-fs');
var streamify = require('streamify-string');
var postcss = require('postcss');


server.listen(5000, function() {
  console.log('Telnet server is running on port', server.address().port); 
});

function afterSend() {
  console.log("data sent!");
}

server.on('connection', function(socket) {

  console.log('connecting..');

  socket.on('end', function(){
    console.log('disconnecting');
  });

  socket.setNoDelay(true);

  socket.on('data', function(data) {

    socket.setEncoding('utf8');

    // should only be 1 line at a time
    var css = data; //.toString().replace(/(\r\n|\n|\r)/gm,"");

    // str.replace(/[^A-Za-z 0-9 \.,\?""!@#\$%\^&\*\(\)-_=\+;:<>\/\\\|\}\{\[\]`~]*/g, '')
    console.log('received data as binary' + css.toString('hex'));
    
    console.log('received data as text : ' + css);

    postcss( [ autoprefixer ({ browsers: ['last 4 version'] }) ] )
      .process(css).then(function(result){
          result.warnings().forEach(function (warn) {
            console.warn(warn.toString());
          }); 

          var postProcessedCss = result.css + "<<<"
          console.log("result: " + postProcessedCss);
          var status = socket.write(postProcessedCss, 'utf8', afterSend);
          console.log('did it write all?' + status);
    });

  });

});

/*
    var plugins = [
      autoprefixer({ browsers: ['last 4 version'] })
    ];

    var processCss = function read(file, cb) {
      console.log("processing css ..");
      console.log(JSON.stringify(file));
      console.log("data attribute ..");
      console.log(JSON.stringify(file.data));
      console.log(JSON.stringify(file.type));
      console.log('cb' + JSON.stringify(cb));
      socket.write(file);
//      socket.write(file._contents);
    }

    console.log('data in: ' + data);
    console.log(JSON.stringify(data));
    streamify(data)
      .pipe(postcss(plugins))
      .pipe(map(processCss))
    gulp.src('styles.css').pipe(
    
    );
*/

