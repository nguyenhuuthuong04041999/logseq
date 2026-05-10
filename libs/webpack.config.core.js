const webpack = require('webpack')
const path = require('path')
let BundleAnalyzerPlugin
try { BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin } catch (_) {}

module.exports = (env, argv) => {
  const config = {
    entry: './src/LSPlugin.core.ts',
    devtool: 'eval',
    module: {
      rules: [
        {
          test: /\.tsx?$/,
          use: 'ts-loader',
          exclude: /node_modules/,
        },
      ],
    },
    resolve: {
      extensions: ['.tsx', '.ts', '.js'],
    },
    plugins: [
      new webpack.ProvidePlugin({
        process: 'process/browser',
      }),
    ],
    output: {
      library: 'LSPlugin',
      libraryTarget: 'umd',
      filename: 'lsplugin.core.js',
      path: path.resolve(__dirname, '../resources/js'),
    },
  }

  if (argv.mode === 'production') {
    delete config.devtool
    if (BundleAnalyzerPlugin) config.plugins.push(new BundleAnalyzerPlugin())
  }

  return config
}