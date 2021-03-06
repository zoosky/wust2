var webpack = require('webpack');
var CompressionPlugin = require("compression-webpack-plugin");
var BrotliPlugin = require('brotli-webpack-plugin');
var ClosureCompilerPlugin = require("webpack-closure-compiler");

// Load the config generated by scalajs-bundler
var config = require('./scalajs.webpack.config');

config.plugins = config.plugins || [];

config.plugins.push(new webpack.optimize.UglifyJsPlugin({
    compress: {
        warnings: false
    }
}));
// config.plugins.push(new ClosureCompilerPlugin({
//   compiler: {
//     language_in: 'ECMASCRIPT5',
//     language_out: 'ECMASCRIPT5',
//     compilation_level: 'ADVANCED'
//   },
//   concurrency: 3,
// }));

config.plugins.push(new webpack.DefinePlugin({
  'process.env.NODE_ENV': JSON.stringify('production')
}));

var compressFiles = /\.js$|\.js.map$/;
config.plugins.push(new CompressionPlugin({
  asset: "[path].gz[query]",
  algorithm: "zopfli",
  test: compressFiles,
  minRatio: 0.0
}));

config.plugins.push(new BrotliPlugin({
  asset: '[path].br[query]',
  test: compressFiles,
  minRatio: 0.0
}));



module.exports = config;
