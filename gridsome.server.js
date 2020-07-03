// Server API makes it possible to hook into various parts of Gridsome
// on server-side and add custom data to the GraphQL data layer.
// Learn more: https://gridsome.org/docs/server-api/

// Changes here requires a server restart.
// To restart press CTRL + C in terminal and run `gridsome develop`
// const nodeExternals = require("webpack-node-externals");

module.exports = function(api) {
  //   api.chainWebpack((config, { isServer }) => {
  //     if (isServer) {
  //       config.externals([
  //         nodeExternals({
  //           whitelist: [/^vuetify/],
  //         }),
  //       ]);
  //     }
  //   });
  //   api.loadSource(({ addCollection }) => {
  //     // Use the Data store API here: https://gridsome.org/docs/data-store-api/
  //   });

  api.loadSource(({ addSchemaTypes, schema }) => {
    // addSchemaTypes([
    //   schema.createObjectType({
    //     name: "Post",
    //     interfaces: ["Node"],
    //     fields: {
    //       title: "String",
    //       date: "Date",
    //       published: "Boolean",
    //       tags: "String",
    //       series: "Boolean",
    //       cover_image: "String",
    //       canonical_url: "String",
    //       description: "String",
    //       finalFiles: "String",
    //     },
    //   }),
    // ]);
  });
};
