const gulp = require('gulp');
const traceur = require('gulp-traceur');
const watch = require('gulp-watch');
const imagemin = require('gulp-imagemin');
const pngquant = require('imagemin-pngquant');
const uglify = require('gulp-uglify');
const cleanCSS = require('gulp-clean-css');
const rename = require('gulp-rename');

 
gulp.task('js', () => {
	return gulp.src('webview/src/js/*.js')
		.pipe(traceur())
    .pipe(uglify())
    .pipe(rename({suffix: '.min'}))
		.pipe(gulp.dest('webview/public/js/'));
});

gulp.task('css', () => {
  return gulp.src('webview/src/css/*.css')
    .pipe(cleanCSS())
    .pipe(rename({suffix: '.min'}))
    .pipe(gulp.dest('webview/public/css'))
});

gulp.task('images', () => {
    return gulp.src('webview/src/images/*.png')
        .pipe(imagemin({
            progressive: true,
            svgoPlugins: [
                {removeViewBox: false},
                {cleanupIDs: false}
            ],
            use: [pngquant()]
        }))
        .pipe(gulp.dest('webview/public/img'));
});

gulp.task('default', ['js', 'css', 'images']);
