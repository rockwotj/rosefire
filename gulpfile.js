'use strict';

var gulp = require('gulp');
var uglify = require('gulp-uglify');
var gutil = require('gulp-util');
var rename = require("gulp-rename");

gulp.task('default', function () {
  return gulp.src('./rosefire.js')
    .pipe(uglify())
    .pipe(rename({suffix: ".min"}))
    .on('error', gutil.log)
    .pipe(gulp.dest('./dist/js/'));
});
