const { defineConfig } = require("cypress");

module.exports = defineConfig({
  projectId: "pmqp4z",
  component: {
    devServer: {
      framework: "next",
      bundler: "webpack",
    },
  },
});
